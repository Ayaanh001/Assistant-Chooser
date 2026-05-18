package com.hussain.assistantchooser.main

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.*
import com.hussain.assistantchooser.data.launchAssistantForPackage
import com.hussain.assistantchooser.services.QuickLaunchTileService
import com.hussain.assistantchooser.settings.SettingsActivity
import com.hussain.assistantchooser.ui.components.ChangelogBottomSheet
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainContent() }
    }

    @Composable
    private fun MainContent() {
        // Collect these with collectAsState() so they update even when in background
        val themeMode by viewModel.themeMode.collectAsState()
        val appFilterMode by viewModel.appFilterMode.collectAsState()
        val showPackageName by viewModel.showPackageName.collectAsState()
        val openApp by viewModel.openApp.collectAsState()
        val closeAfter by viewModel.closeAfterLaunch.collectAsState()
        val themedIconMode by viewModel.themedIconMode.collectAsState()

        val darkTheme = when (themeMode) {
            ThemeMode.AUTO -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        AssistantChooserTheme(darkTheme = darkTheme) {
            val assistantApps by viewModel.assistantApps.collectAsStateWithLifecycle()
            val allApps by viewModel.allApps.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val savedCustomApps by viewModel.savedCustomApps.collectAsStateWithLifecycle()

            var selectedPackage by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val prefs = remember { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

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

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                AssistantChooserScreen(
                    voiceAssistants = assistantApps,
                    allApps = allApps,
                    isLoading = isLoading,
                    selectedPackage = selectedPackage,
                    appFilterMode = appFilterMode,
                    onAppFilterModeChange = { mode -> viewModel.setAppFilterMode(mode) },
                    onAppClick = { pkg ->
                        if (pkg == packageName) return@AssistantChooserScreen
                        if (openApp) {
                            runCatching {
                                val launch = packageManager.getLaunchIntentForPackage(pkg)
                                if (launch != null) {
                                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launch)
                                } else launchAssistantForPackage(context, pkg)
                            }.onFailure { launchAssistantForPackage(context, pkg) }
                        } else {
                            launchAssistantForPackage(context, pkg)
                        }
                        if (closeAfter) finish()
                    },
                    onSettingsClick = {
                        startActivity(
                            Intent(context, SettingsActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    onAddTileClicked = { requestAddQuickSettingsTile() },
                    onSaveCustomApps = { pkgs -> viewModel.saveCustomApps(pkgs) },
                    savedCustomApps = savedCustomApps,
                    openApp = openApp,
                    closeAfterLaunch = closeAfter,
                    showPackageName = showPackageName,
                    themedIcons = themedIconMode == ThemedIconMode.APP_ONLY || themedIconMode == ThemedIconMode.BOTH
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
            val uri = Settings.Secure.getUriFor("assistant")
            val observer = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
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
                "Swipe down → tap Edit (pencil) → drag 'Assistant Chooser' into active tiles.",
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
                    val message = when (result) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Assistant Chooser Tile added to Quick Settings"
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Assistant Chooser Tile is already added to Quick Settings"
                        else -> "Tile add request failed."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "requestAddTileService failed", e)
        }
    }
}
