package com.hussain.assistantchooser

import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hussain.assistantchooser.com.hussain.assistantchooser.AppFilterMode
import java.util.concurrent.Executors
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf as composeMutableStateOf

class MainActivity : ComponentActivity() {

    private val showPackageNameState = composeMutableStateOf(true)
    private val openAppState = composeMutableStateOf(false)
    private val closeAfterLaunchState = composeMutableStateOf(true)
    private val appFilterModeState = composeMutableStateOf(AppFilterMode.VOICE_ASSISTANTS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        openAppState.value = prefs.getBoolean(KEY_OPEN_APP, false)
        closeAfterLaunchState.value = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
        showPackageNameState.value = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true)

        val savedModeName = prefs.getString(KEY_APP_FILTER_MODE, AppFilterMode.VOICE_ASSISTANTS.name)
        appFilterModeState.value = savedModeName?.let {
            try {
                AppFilterMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                AppFilterMode.VOICE_ASSISTANTS
            }
        } ?: AppFilterMode.VOICE_ASSISTANTS

        val pm = packageManager

        val assistIntent = Intent(Intent.ACTION_ASSIST)
        val assistResolveInfos = pm.queryIntentActivities(assistIntent, PackageManager.MATCH_ALL)

        val voiceIntent = Intent("android.service.voice.VoiceInteractionService")
        val voiceResolveInfos = pm.queryIntentServices(voiceIntent, PackageManager.MATCH_ALL)

        val assistApps = assistResolveInfos.map {
            val appInfo = it.activityInfo.applicationInfo
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AssistantApp(label, appInfo.packageName, icon)
        }

        val voiceApps = voiceResolveInfos.map {
            val appInfo = it.serviceInfo.applicationInfo
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AssistantApp(label, appInfo.packageName, icon)
        }

        val voiceAssistants = (assistApps + voiceApps)
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }

        // Get all installed apps (used only for Custom Apps)
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                val label = pm.getApplicationLabel(it).toString()
                val icon = pm.getApplicationIcon(it)
                AssistantApp(label, it.packageName, icon)
            }
            .sortedBy { it.name.lowercase() }

        // Pre-cache icon bitmaps on background thread (your AssistantApp likely lazily converts drawables)
        Executors.newSingleThreadExecutor().execute {
            voiceAssistants.forEach { try { it.iconBitmap } catch (_: Exception) { } }
        }

        setContent {
            AssistantChooserTheme {
                val systemUiController = rememberSystemUiController()
                val darkIcons = !isSystemInDarkTheme()
                val statusBarColor = Color.Transparent

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = statusBarColor,
                        darkIcons = darkIcons
                    )
                }

                // hoisted Compose state for currently selected package (system default)
                var selectedPackage by remember { mutableStateOf<String?>(null) }
                val context = LocalContext.current

                // OBSERVE system default assistant and update selectedPackage automatically
                RememberObserveDefaultAssistant { pkg ->
                    // pkg is normalized package name or null
                    selectedPackage = pkg
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AssistantChooserScreen(
                        voiceAssistants = voiceAssistants,
                        allApps = allApps,
                        selectedPackage = selectedPackage,
                        appFilterMode = appFilterModeState.value,
                        onAppFilterModeChange = { mode ->
                            appFilterModeState.value = mode
                            prefs.edit().putString(KEY_APP_FILTER_MODE, mode.name).apply()
                        },
                        onAppSelected = { selectedApp ->
                            // keep existing behavior: open system settings to let user pick default
                            selectedPackage = selectedApp.packageName

                            val settingsIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(settingsIntent)

                            Toast.makeText(
                                context,
                                "Select \"${selectedApp.name}\" as your default assistant",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onAppClick = { pkg ->
                            if (openAppState.value) {
                                try {
                                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    } else {
                                        launchAssistantForPackage(context, pm, pkg)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error launching app for $pkg", e)
                                    launchAssistantForPackage(context, pm, pkg)
                                }
                            } else {
                                launchAssistantForPackage(context, pm, pkg)
                            }

                            if (closeAfterLaunchState.value) {
                                finish()
                            }
                        },
                        onSettingsClick = {
                            val intent = Intent(context, SettingsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        onAddTileClicked = {
                            requestAddQuickSettingsTile()
                        },
                        onSaveCustomApps = { customPackages ->
                            prefs.edit().putStringSet(KEY_CUSTOM_APPS, customPackages.toSet()).apply()
                        },
                        savedCustomApps = prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet(),
                        openApp = openAppState.value,
                        closeAfterLaunch = closeAfterLaunchState.value,
                        showPackageName = showPackageNameState.value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        openAppState.value = prefs.getBoolean(KEY_OPEN_APP, false)
        closeAfterLaunchState.value = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
        showPackageNameState.value = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true)

        val savedModeName = prefs.getString(KEY_APP_FILTER_MODE, AppFilterMode.VOICE_ASSISTANTS.name)
        appFilterModeState.value = savedModeName?.let {
            try {
                AppFilterMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                AppFilterMode.VOICE_ASSISTANTS
            }
        } ?: AppFilterMode.VOICE_ASSISTANTS
    }

    private fun requestAddQuickSettingsTile() {
        val context = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBar = getSystemService(StatusBarManager::class.java)
            if (statusBar == null) {
                Toast.makeText(context, "Cannot access StatusBarManager", Toast.LENGTH_SHORT).show()
                return
            }

            val tileComponent = ComponentName(context, QuickLaunchTileService::class.java)
            val labelResId = resources.getIdentifier("tile_label", "string", packageName)
            val tileLabel = if (labelResId != 0) getString(labelResId) else "Assistant Chooser"
            val tileIcon = try {
                Icon.createWithResource(context, R.drawable.qs_tile)
            } catch (e: Exception) {
                Icon.createWithResource(context, android.R.mipmap.sym_def_app_icon)
            }

            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

            try {
                statusBar.requestAddTileService(
                    tileComponent,
                    tileLabel,
                    tileIcon,
                    executor,
                    java.util.function.Consumer { result ->
                        val success = when (result) {
                            is Boolean -> result
                            is java.lang.Boolean -> result.booleanValue()
                            is Int -> result != 0
                            is java.lang.Integer -> result.toInt() != 0
                            else -> false
                        }

                        runOnUiThread {
                            if (success) {
                                Toast.makeText(context, "Assistant Chooser tile added to Quick Settings", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Assistant Chooser Tile add request failed or was denied.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "requestAddTileService failed", e)
                runOnUiThread {
                    Toast.makeText(context, "Failed to request tile placement: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(
                context,
                "To add the Quick Settings tile: open Quick Settings (swipe down), tap Edit (pencil) and drag the 'Launch Assistant' tile into your active tiles.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun launchAssistantForPackage(
        context: android.content.Context,
        pm: PackageManager,
        pkg: String
    ) {
        try {
            if (pkg == "com.google.android.googlequicksearchbox") {
                val methods = listOf(
                    Intent("android.intent.action.VOICE_ASSIST").apply {
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent(Intent.ACTION_SEARCH).apply {
                        setPackage(pkg)
                        putExtra("com.google.android.googlequicksearchbox.EXTRA_VOICE_SEARCH", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent().apply {
                        component = ComponentName(pkg, "com.google.android.voicesearch.VoiceSearchActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent("com.google.android.googlequicksearchbox.GOOGLE_ASSISTANT").apply {
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )

                for (intent in methods) {
                    try {
                        context.startActivity(intent)
                        Log.d("MainActivity", "Successfully launched Google Assistant")
                        return
                    } catch (e: ActivityNotFoundException) {
                        Log.d("MainActivity", "Method failed, trying next...")
                        continue
                    }
                }

                Toast.makeText(
                    context,
                    "Google Assistant not available. Please ensure it's enabled in system settings.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            if (pkg == "com.openai.chatgpt") {
                try {
                    val componentName = ComponentName(
                        "com.openai.chatgpt",
                        "com.openai.voice.assistant.AssistantActivity"
                    )
                    val intent = Intent().apply {
                        component = componentName
                        action = Intent.ACTION_ASSIST
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to launch ChatGPT AssistantActivity", e)
                }
            }

            val assistResolvers = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_ASSIST),
                PackageManager.MATCH_ALL
            )
            val assistRi = assistResolvers.firstOrNull { it.activityInfo.packageName == pkg }

            if (assistRi != null) {
                val comp = ComponentName(assistRi.activityInfo.packageName, assistRi.activityInfo.name)
                val targeted = Intent(Intent.ACTION_ASSIST).apply {
                    component = comp
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(targeted)
                return
            }

            val voiceCmdResolvers = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_VOICE_COMMAND),
                PackageManager.MATCH_ALL
            )
            val voiceCmdRi = voiceCmdResolvers.firstOrNull { it.activityInfo.packageName == pkg }
            if (voiceCmdRi != null) {
                val comp = ComponentName(voiceCmdRi.activityInfo.packageName, voiceCmdRi.activityInfo.name)
                val targeted = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                    component = comp
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(targeted)
                return
            }

            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "No launchable activity found", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching assistant for $pkg", e)
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
        }
    }

    @Composable
    fun RememberObserveDefaultAssistant(onDefaultAssistantChanged: (String?) -> Unit) {
        val context = LocalContext.current
        val resolver = remember { context.contentResolver }

        DisposableEffect(resolver) {

            fun readAndEmit() {
                // Use literal key instead of Settings.Secure.ASSISTANT
                val raw = Settings.Secure.getString(resolver, "assistant")
                val pkg = try {
                    ComponentName.unflattenFromString(raw)?.packageName ?: raw
                } catch (e: Exception) {
                    raw
                }

                val normalized = pkg
                    ?.takeIf { it.isNotBlank() && it != "none" && it != "ITEM_NONE_VALUE" }

                onDefaultAssistantChanged(normalized)
            }

            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    readAndEmit()
                }
            }

            // Also use literal key here
            val assistantUri = Settings.Secure.getUriFor("assistant")

            resolver.registerContentObserver(assistantUri, false, observer)

            readAndEmit()

            onDispose {
                resolver.unregisterContentObserver(observer)
            }
        }
    }
}
