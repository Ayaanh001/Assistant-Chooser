package com.hussain.assistantchooser.main

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.AppFilterMode
import com.hussain.assistantchooser.core.KEY_APP_FILTER_MODE
import com.hussain.assistantchooser.core.KEY_CLOSE_AFTER_LAUNCH
import com.hussain.assistantchooser.core.KEY_FIRST_LAUNCH
import com.hussain.assistantchooser.core.KEY_OPEN_APP
import com.hussain.assistantchooser.core.KEY_SHOW_PACKAGE_NAME
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.data.launchAssistantForPackage
import com.hussain.assistantchooser.services.QuickLaunchTileService
import com.hussain.assistantchooser.settings.SettingsActivity
import com.hussain.assistantchooser.ui.components.ChangelogBottomSheet
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val showPackageNameState  = mutableStateOf(true)
    private val openAppState          = mutableStateOf(false)
    private val closeAfterLaunchState = mutableStateOf(true)
    private val appFilterModeState    = mutableStateOf(AppFilterMode.VOICE_ASSISTANTS)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadPrefs()
        setContent { MainContent() }
    }

    override fun onResume() {
        super.onResume()
        loadPrefs()
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        openAppState.value          = prefs.getBoolean(KEY_OPEN_APP, false)
        closeAfterLaunchState.value = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
        showPackageNameState.value  = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true)
        appFilterModeState.value    = AppFilterMode.valueOf(
            prefs.getString(KEY_APP_FILTER_MODE, AppFilterMode.VOICE_ASSISTANTS.name)
                ?: AppFilterMode.VOICE_ASSISTANTS.name
        )
    }

    @Composable
    private fun MainContent() {
        val pm = packageManager

        AssistantChooserTheme {
            val assistantApps   by viewModel.assistantApps.collectAsStateWithLifecycle()
            val allApps         by viewModel.allApps.collectAsStateWithLifecycle()
            val isLoading       by viewModel.isLoading.collectAsStateWithLifecycle()
            val savedCustomApps by viewModel.savedCustomApps.collectAsStateWithLifecycle()

            var selectedPackage by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val prefs   = remember { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

            var showChangelog by remember {
                mutableStateOf(prefs.getBoolean(KEY_FIRST_LAUNCH, true))
            }

            if (showChangelog) {
                ChangelogBottomSheet(onDismiss = {
                    showChangelog = false
                    prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                })
            }

            ObserveDefaultAssistant { pkg -> selectedPackage = pkg }

            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AssistantChooserScreen(
                        voiceAssistants       = assistantApps,
                        allApps               = allApps,
                        isLoading             = isLoading,
                        selectedPackage       = selectedPackage,
                        appFilterMode         = appFilterModeState.value,
                        onAppFilterModeChange = { mode ->
                            appFilterModeState.value = mode
                            prefs.edit().putString(KEY_APP_FILTER_MODE, mode.name).apply()
                        },
                        onAppClick = { pkg ->
                            if (pkg == packageName) return@AssistantChooserScreen
                            if (openAppState.value) {
                                runCatching {
                                    val launch = pm.getLaunchIntentForPackage(pkg)
                                    if (launch != null) {
                                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launch)
                                    } else launchAssistantForPackage(context, pkg)
                                }.onFailure { launchAssistantForPackage(context, pkg) }
                            } else {
                                launchAssistantForPackage(context, pkg)
                            }
                            if (closeAfterLaunchState.value) finish()
                        },
                        onSettingsClick  = {
                            startActivity(
                                Intent(context, SettingsActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        onAddTileClicked = { requestAddQuickSettingsTile() },
                        onSaveCustomApps = { pkgs -> viewModel.saveCustomApps(pkgs) },
                        savedCustomApps  = savedCustomApps,
                        openApp          = openAppState.value,
                        closeAfterLaunch = closeAfterLaunchState.value,
                        showPackageName  = showPackageNameState.value
                    )
            }
        }
    }

    @Composable
    private fun ObserveDefaultAssistant(onChanged: (String?) -> Unit) {
        val resolver = remember { contentResolver }
        DisposableEffect(resolver) {
            fun read() {
                val raw = Settings.Secure.getString(resolver, "assistant")
                val pkg = runCatching {
                    ComponentName.unflattenFromString(raw)?.packageName ?: raw
                }.getOrNull() ?: raw
                onChanged(pkg?.takeIf { it.isNotBlank() && it != "none" })
            }
            val uri      = Settings.Secure.getUriFor("assistant")
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) = read()
            }
            resolver.registerContentObserver(uri, false, observer)
            read()
            onDispose { resolver.unregisterContentObserver(observer) }
        }
    }

    private fun requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(
                this,
                "Swipe down → tap Edit (pencil) → drag 'Launch Assistant' into active tiles.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val statusBar = getSystemService(StatusBarManager::class.java) ?: return
        val icon = runCatching { Icon.createWithResource(this, R.drawable.qs_tile) }
            .getOrElse { Icon.createWithResource(this, android.R.mipmap.sym_def_app_icon) }
        try {
            statusBar.requestAddTileService(
                ComponentName(this, QuickLaunchTileService::class.java),
                getString(R.string.app_name), icon,
                java.util.concurrent.Executors.newSingleThreadExecutor()
            ) { result ->
                runOnUiThread {
                    val ok = when (result) {
                        is Int     -> result != 0
                        is Boolean -> result
                        else       -> false
                    }
                    Toast.makeText(
                        this,
                        if (ok) "Tile added to Quick Settings" else "Tile add request failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "requestAddTileService failed", e)
        }
    }
}
