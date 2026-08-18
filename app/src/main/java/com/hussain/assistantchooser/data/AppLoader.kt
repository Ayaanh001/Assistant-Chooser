package com.hussain.assistantchooser.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import com.hussain.assistantchooser.core.AppCache
import com.hussain.assistantchooser.core.AssistantApp
import org.xmlpull.v1.XmlPullParser

/**
 * Queries the package manager and launcher apps for assistant and all launchable apps/shortcuts,
 * then populates [AppCache]. Intended to be called on a background thread.
 */
fun loadApps(context: Context) {
    val pm = context.packageManager
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val assistIntent = Intent(Intent.ACTION_ASSIST)
    val voiceIntent  = Intent("android.service.voice.VoiceInteractionService")

    val assistApps = pm
        .queryIntentActivities(assistIntent, PackageManager.MATCH_ALL)
        .map {
            val ai = it.activityInfo.applicationInfo
            val label = pm.getApplicationLabel(ai).toString()
            AssistantApp(
                name        = label,
                packageName = ai.packageName,
                icon        = pm.getApplicationIcon(ai),
                appName     = label,
            )
        }

    val voiceApps = pm
        .queryIntentServices(voiceIntent, PackageManager.MATCH_ALL)
        .map {
            val ai = it.serviceInfo.applicationInfo
            val label = pm.getApplicationLabel(ai).toString()
            AssistantApp(
                name        = label,
                packageName = ai.packageName,
                icon        = pm.getApplicationIcon(ai),
                appName     = label,
            )
        }

    val assistantApps = (assistApps + voiceApps)
        .distinctBy { it.packageName }
        .sortedBy   { it.name.lowercase() }

    // 1. Get all launchable apps using LauncherApps (more reliable for multi-user/profiles)
    val appsList = mutableListOf<AssistantApp>()
    runCatching {
        launcherApps.profiles.forEach { user ->
            launcherApps.getActivityList(null, user).forEach { info ->
                appsList.add(
                    AssistantApp(
                        name         = info.label.toString(),
                        packageName  = info.applicationInfo.packageName,
                        icon         = info.getIcon(0),
                        appName      = info.label.toString(),
                        activityName = info.componentName.className,
                        userHandle   = user
                    )
                )
            }
        }
    }.onFailure {
        android.util.Log.e("AppLoader", "Failed to load apps via LauncherApps", it)
        // Fallback to PackageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL).forEach {
            val ai = it.activityInfo.applicationInfo
            val label = pm.getApplicationLabel(ai).toString()
            appsList.add(
                AssistantApp(
                    name         = label,
                    packageName  = ai.packageName,
                    icon         = pm.getApplicationIcon(ai),
                    appName      = label,
                    activityName = it.activityInfo.name
                )
            )
        }
    }

    val shortcutsMap = mutableMapOf<String, AssistantApp>()

    val hasShortcutPermission = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)
    android.util.Log.d("AppLoader", "hasShortcutHostPermission: $hasShortcutPermission")

    // 2. Thoroughly parse static shortcuts from manifest meta-data
    runCatching {
        val allPackages = pm.getInstalledPackages(0)
        val tempShortcuts = mutableListOf<AssistantApp>()
        
        fun parsePkg(pkgName: String, meta: android.os.Bundle?) {
            if (meta?.containsKey("android.app.shortcuts") == true) {
                val resId = meta.getInt("android.app.shortcuts")
                parseStaticShortcuts(context, pkgName, resId, tempShortcuts)
            }
        }

        for (pkgInfo in allPackages) {
            // Query detailed info for each package one by one to avoid TransactionTooLargeException
            val detailedPkg = runCatching { 
                pm.getPackageInfo(pkgInfo.packageName, PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES) 
            }.getOrNull() ?: continue

            // Check application-level shortcuts
            parsePkg(detailedPkg.packageName, detailedPkg.applicationInfo?.metaData)
            
            // Check every activity for shortcuts (many apps like PhonePe do this)
            detailedPkg.activities?.forEach { activityInfo ->
                parsePkg(detailedPkg.packageName, activityInfo.metaData)
            }
        }

        val primaryUser = android.os.Process.myUserHandle()
        tempShortcuts.forEach { 
            // Ensure static shortcuts from manifest have a user-aware key to match LauncherApps results
            val key = it.copy(userHandle = primaryUser).key
            shortcutsMap[key] = it.copy(userHandle = primaryUser)
        }
    }.onFailure {
        android.util.Log.e("AppLoader", "Failed to parse shortcuts from manifest", it)
    }

    if (hasShortcutPermission) {
        runCatching {
            launcherApps.profiles.forEach { user ->
                val query = LauncherApps.ShortcutQuery().apply {
                    var flags = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        flags = flags or LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
                    }
                    setQueryFlags(flags)
                }
                val shortcuts = launcherApps.getShortcuts(query, user) ?: emptyList()
                shortcuts.forEach { shortcut ->
                    if (!shortcut.isEnabled) return@forEach
                    val key = AssistantApp(
                        name = "", packageName = shortcut.`package`, icon = pm.defaultActivityIcon, 
                        shortcutId = shortcut.id, userHandle = user
                    ).key
                    
                    // Prefer manifest-parsed shortcut if it has intents (better launch reliability)
                    if (shortcutsMap[key]?.intents != null) return@forEach

                    val parentIcon = runCatching { pm.getApplicationIcon(shortcut.`package`) }.getOrNull()
                    val icon = runCatching {
                        launcherApps.getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi)
                    }.getOrNull() ?: parentIcon ?: pm.getApplicationIcon(shortcut.`package`)

                    val appLabel = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(shortcut.`package`, 0)).toString()
                    }.getOrNull() ?: shortcut.`package`

                    shortcutsMap[key] = AssistantApp(
                        name = (shortcut.shortLabel ?: shortcut.longLabel ?: "Shortcut").toString(),
                        packageName = shortcut.`package`,
                        icon = icon,
                        shortcutId = shortcut.id,
                        userHandle = user,
                        appName = appLabel,
                        parentIcon = parentIcon
                    )
                }
            }
        }.onFailure {
            android.util.Log.e("AppLoader", "Failed to load shortcuts via LauncherApps", it)
        }
    }

    val shortcutsList = shortcutsMap.values.toList()

    // Include assistantApps in allApps so they can be selected even if they lack a launcher activity
    val allApps = (appsList + assistantApps + shortcutsList)
        .distinctBy { it.key }
        .sortedBy   { it.name.lowercase() }

    android.util.Log.d("AppLoader", "Total apps + shortcuts: ${allApps.size}")

    // Pre-warm bitmap conversion so overlay launch is truly instant
    assistantApps.forEach { runCatching { it.iconBitmap } }
    allApps.forEach { runCatching { it.iconBitmap } }

    AppCache.populate(assistantApps, allApps)
}

private fun parseStaticShortcuts(
    context: Context,
    packageName: String,
    resId: Int,
    outList: MutableList<AssistantApp>
) {
    val pm = context.packageManager
    runCatching {
        val resources = pm.getResourcesForApplication(packageName)
        val parser = resources.getXml(resId)
        var eventType = parser.eventType
        var currentShortcutId: String? = null
        var currentLabel: String? = null
        var currentIconRes = 0
        val currentIntents = mutableListOf<Intent>()
        
        val parentIcon = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()

        val appLabel = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull() ?: packageName

        var foundCount = 0
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "shortcut" -> {
                        currentShortcutId = getAttributeValue(parser, resources, "shortcutId")
                        val labelRes = getAttributeResourceValue(parser, "shortcutShortLabel")
                        currentLabel = if (labelRes != 0) resources.getString(labelRes) 
                                      else getAttributeValue(parser, resources, "shortcutShortLabel")
                        
                        // Fallback for long label if short is missing
                        if (currentLabel == null) {
                            val longLabelRes = getAttributeResourceValue(parser, "shortcutLongLabel")
                            currentLabel = if (longLabelRes != 0) resources.getString(longLabelRes)
                                          else getAttributeValue(parser, resources, "shortcutLongLabel")
                        }
                        
                        currentIconRes = getAttributeResourceValue(parser, "icon")
                        currentIntents.clear()
                        android.util.Log.d("AppLoader", "Found shortcut tag: id=$currentShortcutId, label=$currentLabel")
                    }
                    "intent" -> {
                        val action = getAttributeValue(parser, resources, "action")
                        val targetPkg = getAttributeValue(parser, resources, "targetPackage")
                        val targetCls = getAttributeValue(parser, resources, "targetClass")
                        val data = getAttributeValue(parser, resources, "data")
                        
                        val intent = action?.let { Intent(it) } ?: Intent()
                        intent.apply {
                            if (targetPkg != null && targetCls != null) {
                                setClassName(targetPkg, targetCls)
                            } else if (targetPkg != null) {
                                setPackage(targetPkg)
                            } else {
                                // Explicitly set the parent package if targetPackage is missing
                                // This helps resolve shortcuts that only specify a targetClass or just an action
                                setPackage(packageName)
                            }
                            if (data != null) {
                                setData(android.net.Uri.parse(data))
                            }
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addCategory(Intent.CATEGORY_DEFAULT)
                        }
                        currentIntents.add(intent)
                        android.util.Log.d("AppLoader", "  Added intent: action=$action, target=$targetPkg/$targetCls")
                    }
                    "category" -> {
                        val name = getAttributeValue(parser, resources, "name")
                        if (name != null) {
                            currentIntents.lastOrNull()?.addCategory(name)
                            android.util.Log.d("AppLoader", "    Added category: $name")
                        }
                    }
                    "extra" -> {
                        val name = getAttributeValue(parser, resources, "name")
                        val value = getAttributeValue(parser, resources, "value")
                        if (name != null && value != null) {
                            currentIntents.lastOrNull()?.putExtra(name, value)
                            android.util.Log.d("AppLoader", "    Added extra: $name=$value")
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == "shortcut") {
                    if (currentShortcutId != null && currentLabel != null) {
                        foundCount++
                        val icon = if (currentIconRes != 0) {
                            runCatching { resources.getDrawable(currentIconRes, null) }.getOrNull()
                        } else null
                        
                        val app = AssistantApp(
                            name = currentLabel,
                            packageName = packageName,
                            icon = icon ?: parentIcon ?: pm.getApplicationIcon(packageName),
                            shortcutId = currentShortcutId,
                            appName = appLabel,
                            intents = if (currentIntents.isNotEmpty()) ArrayList(currentIntents) else null,
                            parentIcon = parentIcon
                        )
                        outList.add(app)
                        android.util.Log.d("AppLoader", "Finalized shortcut: ${app.key}")
                    }
                    currentShortcutId = null
                    currentLabel = null
                    currentIconRes = 0
                    currentIntents.clear()
                }
            }
            eventType = parser.next()
        }
        android.util.Log.d("AppLoader", "Parsed $foundCount static shortcuts for $packageName")
    }
}

private fun getAttributeValue(parser: XmlResourceParser, resources: android.content.res.Resources, name: String): String? {
    val resId = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", name, 0)
    if (resId != 0) {
        return runCatching { resources.getString(resId) }.getOrNull()
    }
    return parser.getAttributeValue("http://schemas.android.com/apk/res/android", name)
        ?: parser.getAttributeValue(null, name)
}

private fun getAttributeResourceValue(parser: XmlResourceParser, name: String): Int {
    return parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", name, 0)
        .takeIf { it != 0 }
        ?: parser.getAttributeResourceValue(null, name, 0)
}
