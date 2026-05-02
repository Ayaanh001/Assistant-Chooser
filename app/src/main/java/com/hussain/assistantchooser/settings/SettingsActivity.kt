package com.hussain.assistantchooser.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hussain.assistantchooser.core.*
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
            AssistantChooserTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var openApp by remember { mutableStateOf(prefs.getBoolean(KEY_OPEN_APP, false)) }
                    var closeAfter by remember { mutableStateOf(prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)) }
                    var showPkg by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true)) }
                    var showAppName by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_APP_NAME, true)) }
                    var tileOpenOverlay by remember { mutableStateOf(prefs.getBoolean(KEY_TILE_OPEN_OVERLAY, true)) }
                    var overlaySrc by remember {
                        mutableStateOf(OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null)))
                    }

                    SettingsScreen(
                        initialOpenApp              = openApp,
                        initialCloseAfter           = closeAfter,
                        initialShowPackage          = showPkg,
                        initialShowAppName          = showAppName,
                        initialOverlaySrc           = overlaySrc,
                        initialTileOpenOverlay      = tileOpenOverlay,
                        onToggleOpenApp             = {
                            openApp = it
                            prefs.edit().putBoolean(KEY_OPEN_APP, it).apply()
                        },
                        onToggleCloseAfter          = {
                            closeAfter = it
                            prefs.edit().putBoolean(KEY_CLOSE_AFTER_LAUNCH, it).apply()
                        },
                        onToggleShowPackageName     = {
                            showPkg = it
                            prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAME, it).apply()
                        },
                        onOverlaySourceChange       = {
                            overlaySrc = it
                            prefs.edit().putString(KEY_OVERLAY_SOURCE, it.name).apply()
                        },
                        onToggleShowAppName         = {
                            showAppName = it
                            prefs.edit().putBoolean(KEY_SHOW_APP_NAME, it).apply()
                        },
                        onToggleTileOpenOverlay     = {
                            tileOpenOverlay = it
                            prefs.edit().putBoolean(KEY_TILE_OPEN_OVERLAY, it).apply()
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
                                tileOpenOverlay = prefs.getBoolean(KEY_TILE_OPEN_OVERLAY, tileOpenOverlay)
                                overlaySrc = OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null))
                                
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