package com.hussain.assistantchooser.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hussain.assistantchooser.core.KEY_CLOSE_AFTER_LAUNCH
import com.hussain.assistantchooser.core.KEY_OPEN_APP
import com.hussain.assistantchooser.core.KEY_OVERLAY_SOURCE
import com.hussain.assistantchooser.core.KEY_SHOW_APP_NAME
import com.hussain.assistantchooser.core.KEY_SHOW_PACKAGE_NAME
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            AssistantChooserTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        initialOpenApp              = prefs.getBoolean(KEY_OPEN_APP, false),
                        initialCloseAfter           = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true),
                        initialShowPackage          = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true),
                        initialShowAppName          = prefs.getBoolean(KEY_SHOW_APP_NAME, true),
                        initialOverlaySrc           = OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null)),
                        onToggleOpenApp             = { prefs.edit().putBoolean(KEY_OPEN_APP, it).apply() },
                        onToggleCloseAfter          = { prefs.edit().putBoolean(KEY_CLOSE_AFTER_LAUNCH, it).apply() },
                        onToggleShowPackageName     = { prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAME, it).apply() },
                        onOverlaySourceChange       = { prefs.edit().putString(KEY_OVERLAY_SOURCE, it.name).apply() },
                        onToggleShowAppName         = { prefs.edit().putBoolean(KEY_SHOW_APP_NAME, it).apply() },
                        onBack                      = { finish() }
                    )
                }
            }
        }
    }
}
