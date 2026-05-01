package com.hussain.assistantchooser.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussain.assistantchooser.core.*
import com.hussain.assistantchooser.data.launchAssistantForPackage
import com.hussain.assistantchooser.main.MainActivity
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme

class AssistantOverlayActivity : ComponentActivity() {

    private val viewModel: OverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val prefs      = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val showAppName = prefs.getBoolean(KEY_SHOW_APP_NAME, true)
        val openApp    = prefs.getBoolean(KEY_OPEN_APP, false)
        val closeAfter = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)

        setContent {
            AssistantChooserTheme(transparentBackground = true) {
                val apps               by viewModel.apps.collectAsStateWithLifecycle()
                val isLoading          by viewModel.isLoading.collectAsStateWithLifecycle()
                val overlaySource      by viewModel.overlaySource.collectAsStateWithLifecycle()
                val allApps            by viewModel.allApps.collectAsStateWithLifecycle()
                val savedCustomPackages by viewModel.savedCustomPackages.collectAsStateWithLifecycle()

                AssistantOverlayScreen(
                    apps                = apps,
                    isLoading           = isLoading,
                    overlaySource       = overlaySource,
                    allApps             = allApps,
                    savedCustomPackages = savedCustomPackages,
                    showAppName         = showAppName,
                    onAppClick          = { pkg ->
                        if (openApp) {
                            runCatching {
                                val launch = packageManager.getLaunchIntentForPackage(pkg)
                                if (launch != null) {
                                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launch)
                                } else launchAssistantForPackage(this, pkg)
                            }.onFailure { launchAssistantForPackage(this, pkg) }
                        } else {
                            launchAssistantForPackage(this, pkg)
                        }
                        if (closeAfter) finish()
                    },
                    onDismiss           = { finish() },
                    onOpenApp           = { openFullApp() },
                    onSaveCustomApps    = { viewModel.saveCustomApps(it) },
                )
            }
        }
    }

    private fun openFullApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        startActivity(intent)
        finish()
    }
}
