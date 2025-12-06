package com.hussain.assistantchooser

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

class SettingsActivity : ComponentActivity() {

    private val PREFS_NAME = "assistant_prefs"
    private val KEY_OPEN_APP = "open_app"
    private val KEY_CLOSE_AFTER_LAUNCH = "close_after_launch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // load initial values
        val initialOpenApp = prefs.getBoolean(KEY_OPEN_APP, false)
        val initialCloseAfter = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)


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

                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        initialOpenApp = initialOpenApp,
                        initialCloseAfter = initialCloseAfter,
                        onToggleOpenApp = { enabled ->
                            prefs.edit().putBoolean(KEY_OPEN_APP, enabled).apply()
                        },
                        onToggleCloseAfter = { enabled ->
                            prefs.edit().putBoolean(KEY_CLOSE_AFTER_LAUNCH, enabled).apply()
                        },
                        onBack = { finish() }
                    )
                }
            }
        }

    }
}

data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val checked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialOpenApp: Boolean,
    initialCloseAfter: Boolean,
    onToggleOpenApp: (Boolean) -> Unit,
    onToggleCloseAfter: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var openApp by remember { mutableStateOf(initialOpenApp) }
    var closeAfter by remember { mutableStateOf(initialCloseAfter) }

    val settings = listOf(
        SettingItem(
            id = "open_app",
            title = "Open App",
            subtitle = "Clicking on App name will open app instead of voice Assistant",
            checked = openApp
        ),
        SettingItem(
            id = "close_after",
            title = "Close app after activity launch",
            subtitle = "App will get close after launching an app",
            checked = closeAfter
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .clickable { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(settings) { index, item ->
                    val shape = when {
                        settings.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(
                            topStart = 24.dp, topEnd = 24.dp,
                            bottomStart = 8.dp, bottomEnd = 8.dp
                        )
                        index == settings.size - 1 -> RoundedCornerShape(
                            topStart = 8.dp, topEnd = 8.dp,
                            bottomStart = 24.dp, bottomEnd = 24.dp
                        )
                        else -> RoundedCornerShape(8.dp)
                    }

                    val checked = when (item.id) {
                        "open_app" -> openApp
                        "close_after" -> closeAfter
                        else -> item.checked
                    }

                    val onToggle: (Boolean) -> Unit = when (item.id) {
                        "open_app" -> {
                            { enabled ->
                                openApp = enabled
                                onToggleOpenApp(enabled)
                            }
                        }
                        "close_after" -> {
                            { enabled ->
                                closeAfter = enabled
                                onToggleCloseAfter(enabled)
                            }
                        }
                        else -> { _ -> }
                    }

                    SettingTile(
                        title = item.title,
                        subtitle = item.subtitle,
                        checked = checked,
                        onCheckedChange = onToggle,
                        shape = shape
                    )
                }
            }
        }
    }
}

@Composable
fun SettingTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}