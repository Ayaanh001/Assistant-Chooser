package com.hussain.assistantchooser.data

import android.content.Intent
import android.content.pm.PackageManager
import com.hussain.assistantchooser.core.AppCache
import com.hussain.assistantchooser.core.AssistantApp

/**
 * Queries the package manager for assistant and all launchable apps,
 * then populates [AppCache]. Intended to be called on a background thread.
 */
fun loadApps(pm: PackageManager) {
    val assistIntent = Intent(Intent.ACTION_ASSIST)
    val voiceIntent  = Intent("android.service.voice.VoiceInteractionService")

    val assistApps = pm
        .queryIntentActivities(assistIntent, PackageManager.MATCH_ALL)
        .map {
            val ai = it.activityInfo.applicationInfo
            AssistantApp(
                name        = pm.getApplicationLabel(ai).toString(),
                packageName = ai.packageName,
                icon        = pm.getApplicationIcon(ai)
            )
        }

    val voiceApps = pm
        .queryIntentServices(voiceIntent, PackageManager.MATCH_ALL)
        .map {
            val ai = it.serviceInfo.applicationInfo
            AssistantApp(
                name        = pm.getApplicationLabel(ai).toString(),
                packageName = ai.packageName,
                icon        = pm.getApplicationIcon(ai)
            )
        }

    val assistantApps = (assistApps + voiceApps)
        .distinctBy { it.packageName }
        .sortedBy   { it.name.lowercase() }

    val launchIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val allApps = pm.queryIntentActivities(launchIntent, 0)
        .map {
            val ai = it.activityInfo.applicationInfo
            AssistantApp(
                name        = pm.getApplicationLabel(ai).toString(),
                packageName = ai.packageName,
                icon        = pm.getApplicationIcon(ai)
            )
        }
        .distinctBy { it.packageName }
        .sortedBy   { it.name.lowercase() }

    // Pre-warm bitmap conversion so overlay launch is truly instant
    assistantApps.forEach { runCatching { it.iconBitmap } }
    allApps.forEach { runCatching { it.iconBitmap } }

    AppCache.populate(assistantApps, allApps)
}
