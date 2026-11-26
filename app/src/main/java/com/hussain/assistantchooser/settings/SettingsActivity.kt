package com.hussain.assistantchooser.settings

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()
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

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val overlayApps by viewModel.overlayApps.collectAsState()
            val allApps by viewModel.allApps.collectAsState()
            val appLists by viewModel.appLists.collectAsState()
            val activeListId by viewModel.activeListId.collectAsState()
            val isLoadingApps by viewModel.isLoadingApps.collectAsState()

            val darkTheme = when (uiState.themeMode) {
                ThemeMode.AUTO -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AssistantChooserTheme(darkTheme = darkTheme, useSystemFont = uiState.useSystemFont) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        uiState = uiState,
                        overlayApps = overlayApps,
                        allApps = allApps,
                        appLists = appLists,
                        activeListId = activeListId,
                        isLoadingApps = isLoadingApps,
                        onSaveAllCustomLists = viewModel::saveAllCustomLists,
                        onSetActiveCustomListId = viewModel::setActiveCustomListId,
                        onThemeModeChange = viewModel::setThemeMode,
                        onToggleUseSystemFont = viewModel::setUseSystemFont,
                        onToggleShowPackageName = viewModel::setShowPackageName,
                        onToggleOpenApp = viewModel::setOpenApp,
                        onToggleCloseAfter = viewModel::setCloseAfterLaunch,
                        onOverlaySourceChange = viewModel::setOverlaySource,
                        onHeaderButtonActionChange = viewModel::setHeaderButtonAction,
                        onHeaderButtonLongPressActionChange = viewModel::setHeaderButtonLongPressAction,
                        onToggleShowOverlaySwitch = viewModel::setShowOverlaySwitch,
                        onToggleShowAppName = viewModel::setShowAppName,
                        onToggleTileOpenOverlay = viewModel::setTileOpenOverlay,
                        onToggleSwipeToChangeList = viewModel::setSwipeToChangeList,
                        onThemedIconModeChange = viewModel::setThemedIconMode,
                        onCheckUpdate = viewModel::checkForUpdates,
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
                                viewModel.reloadPrefs()
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
