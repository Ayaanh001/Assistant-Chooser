package com.hussain.assistantchooser.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hussain.assistantchooser.core.KEY_CLOSE_AFTER_LAUNCH
import com.hussain.assistantchooser.core.KEY_OPEN_APP
import com.hussain.assistantchooser.core.KEY_OVERLAY_SOURCE
import com.hussain.assistantchooser.core.KEY_OVERLAY_HEADER_BUTTON
import com.hussain.assistantchooser.core.KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS
import com.hussain.assistantchooser.core.KEY_SHOW_APP_NAME
import com.hussain.assistantchooser.core.KEY_SHOW_OVERLAY_SWITCH
import com.hussain.assistantchooser.core.KEY_SHOW_PACKAGE_NAME
import com.hussain.assistantchooser.core.KEY_SWIPE_TO_CHANGE_LIST
import com.hussain.assistantchooser.core.KEY_THEMED_ICONS
import com.hussain.assistantchooser.core.KEY_THEME_MODE
import com.hussain.assistantchooser.core.KEY_TILE_OPEN_OVERLAY
import com.hussain.assistantchooser.core.KEY_USE_SYSTEM_FONT
import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.core.ThemedIconMode
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : ComponentActivity() {

    private var pendingExportJson: String? = null

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(pendingExportJson ?: "")
                    }
                }
                Toast.makeText(this, "Settings exported successfully", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Failed to export settings", Toast.LENGTH_SHORT).show()
            }
        }
        pendingExportJson = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            var themeMode by remember {
                mutableStateOf(ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null)))
            }

            var useSystemFont by remember {
                mutableStateOf(prefs.getBoolean(KEY_USE_SYSTEM_FONT, false))
            }

            val darkTheme = when (themeMode) {
                ThemeMode.AUTO -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AssistantChooserTheme(darkTheme = darkTheme, useSystemFont = useSystemFont) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var openApp by remember { mutableStateOf(prefs.getBoolean(KEY_OPEN_APP, false)) }
                    var closeAfter by remember { mutableStateOf(prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)) }
                    var showPkg by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, false)) }
                    var showAppName by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_APP_NAME, true)) }
                    var themedIconMode by remember {
                        mutableStateOf(ThemedIconMode.fromString(prefs.getString(KEY_THEMED_ICONS, null)))
                    }
                    var tileOpenOverlay by remember { mutableStateOf(prefs.getBoolean(KEY_TILE_OPEN_OVERLAY, true)) }
                    var overlaySrc by remember {
                        mutableStateOf(OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null)))
                    }
                    var swipeToChangeList by remember { mutableStateOf(prefs.getBoolean(KEY_SWIPE_TO_CHANGE_LIST, true)) }
                    var showOverlaySwitch by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_OVERLAY_SWITCH, true)) }
                    var headerButtonAction by remember {
                        mutableStateOf(OverlayHeaderButtonAction.fromString(prefs.getString(KEY_OVERLAY_HEADER_BUTTON, null)))
                    }
                    var headerButtonLongPressAction by remember {
                        mutableStateOf(
                            OverlayHeaderButtonAction.fromString(
                                prefs.getString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, null),
                                default = OverlayHeaderButtonAction.SETTINGS
                            )
                        )
                    }

                    SettingsScreen(
                        initialOpenApp                      = openApp,
                        initialCloseAfter                   = closeAfter,
                        initialShowPackage                  = showPkg,
                        initialShowAppName                  = showAppName,
                        initialThemedIconMode               = themedIconMode,
                        initialOverlaySrc                   = overlaySrc,
                        initialTileOpenOverlay              = tileOpenOverlay,
                        initialThemeMode                    = themeMode,
                        initialUseSystemFont                = useSystemFont,
                        initialSwipeToChangeList            = swipeToChangeList,
                        initialShowOverlaySwitch            = showOverlaySwitch,
                        initialHeaderButtonAction           = headerButtonAction,
                        initialHeaderButtonLongPressAction  = headerButtonLongPressAction,
                        onToggleOpenApp                     = {
                            openApp = it
                            prefs.edit().putBoolean(KEY_OPEN_APP, it).apply()
                        },
                        onToggleCloseAfter                  = {
                            closeAfter = it
                            prefs.edit().putBoolean(KEY_CLOSE_AFTER_LAUNCH, it).apply()
                        },
                        onToggleShowPackageName             = {
                            showPkg = it
                            prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAME, it).apply()
                        },
                        onOverlaySourceChange               = {
                            overlaySrc = it
                            prefs.edit().putString(KEY_OVERLAY_SOURCE, it.name).apply()
                        },
                        onHeaderButtonActionChange          = {
                            headerButtonAction = it
                            prefs.edit().putString(KEY_OVERLAY_HEADER_BUTTON, it.name).apply()
                        },
                        onHeaderButtonLongPressActionChange = {
                            headerButtonLongPressAction = it
                            prefs.edit().putString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, it.name).apply()
                        },
                        onToggleShowAppName         = {
                            showAppName = it
                            prefs.edit().putBoolean(KEY_SHOW_APP_NAME, it).apply()
                        },
                        onThemedIconModeChange      = {
                            themedIconMode = it
                            prefs.edit().putString(KEY_THEMED_ICONS, it.name).apply()
                        },
                        onToggleTileOpenOverlay     = {
                            tileOpenOverlay = it
                            prefs.edit().putBoolean(KEY_TILE_OPEN_OVERLAY, it).apply()
                        },
                        onToggleSwipeToChangeList   = {
                            swipeToChangeList = it
                            prefs.edit().putBoolean(KEY_SWIPE_TO_CHANGE_LIST, it).apply()
                        },
                        onToggleShowOverlaySwitch   = {
                            showOverlaySwitch = it
                            prefs.edit().putBoolean(KEY_SHOW_OVERLAY_SWITCH, it).apply()
                        },
                        onThemeModeChange           = {
                            themeMode = it
                            prefs.edit().putString(KEY_THEME_MODE, it.name).apply()
                        },
                        onToggleUseSystemFont       = {
                            useSystemFont = it
                            prefs.edit().putBoolean(KEY_USE_SYSTEM_FONT, it).apply()
                        },
                        onExport = { exportCustom, exportSettings ->
                            pendingExportJson = BackupUtils.createExportJson(
                                this@SettingsActivity,
                                exportCustom,
                                exportSettings
                            )
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HHmmss", Locale.getDefault()).format(Date())
                            val fileName = "Assistant Chooser Backup $timestamp.ac"
                            createDocumentLauncher.launch(fileName)
                        },
                        onImport = { json ->
                            if (BackupUtils.importFromJson(this@SettingsActivity, json)) {
                                // Refresh local state after import
                                openApp = prefs.getBoolean(KEY_OPEN_APP, openApp)
                                closeAfter = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, closeAfter)
                                showPkg = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, showPkg)
                                showAppName = prefs.getBoolean(KEY_SHOW_APP_NAME, showAppName)
                                themedIconMode = ThemedIconMode.fromString(prefs.getString(KEY_THEMED_ICONS, null))
                                tileOpenOverlay = prefs.getBoolean(KEY_TILE_OPEN_OVERLAY, tileOpenOverlay)
                                swipeToChangeList = prefs.getBoolean(KEY_SWIPE_TO_CHANGE_LIST, swipeToChangeList)
                                showOverlaySwitch = prefs.getBoolean(KEY_SHOW_OVERLAY_SWITCH, showOverlaySwitch)
                                headerButtonAction = OverlayHeaderButtonAction.fromString(prefs.getString(KEY_OVERLAY_HEADER_BUTTON, null))
                                headerButtonLongPressAction = OverlayHeaderButtonAction.fromString(
                                    prefs.getString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, null),
                                    default = OverlayHeaderButtonAction.SETTINGS
                                )
                                overlaySrc = OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null))
                                themeMode = ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null))
                                useSystemFont = prefs.getBoolean(KEY_USE_SYSTEM_FONT, useSystemFont)
                                
                                Toast.makeText(this@SettingsActivity, "Settings imported successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Failed to import settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}