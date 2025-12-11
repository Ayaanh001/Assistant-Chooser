package com.hussain.assistantchooser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialOpenApp = prefs.getBoolean(KEY_OPEN_APP, false)
        val initialCloseAfter = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
        val initialShowPackage = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true)

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
                        initialShowPackage = initialShowPackage,
                        onToggleOpenApp = { enabled ->
                            prefs.edit().putBoolean(KEY_OPEN_APP, enabled).apply()
                        },
                        onToggleCloseAfter = { enabled ->
                            prefs.edit().putBoolean(KEY_CLOSE_AFTER_LAUNCH, enabled).apply()
                        },
                        onToggleShowPackageName = { enabled ->
                            prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAME, enabled).apply()
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

fun performHapticFeedback(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(50)
        }
    }
}

fun isNewerVersion(latest: String, current: String): Boolean {
    val lParts = latest.split(".").mapNotNull { it.toIntOrNull() }
    val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
    val size = maxOf(lParts.size, cParts.size)
    for (i in 0 until size) {
        val l = lParts.getOrElse(i) { 0 }
        val c = cParts.getOrElse(i) { 0 }
        if (l != c) return l > c
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialOpenApp: Boolean,
    initialCloseAfter: Boolean,
    initialShowPackage: Boolean,
    onToggleOpenApp: (Boolean) -> Unit,
    onToggleCloseAfter: (Boolean) -> Unit,
    onToggleShowPackageName: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var openApp by remember { mutableStateOf(initialOpenApp) }
    var closeAfter by remember { mutableStateOf(initialCloseAfter) }
    var showPackageName by remember { mutableStateOf(initialShowPackage) }

    val currentVersion = "1.2" // keep in sync with your app version if you want
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var updateAvailable by remember { mutableStateOf<Boolean?>(null) } // null = not checked yet
    var loading by remember { mutableStateOf(false) }

    // snackbar state + coroutine scope
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // flags to avoid repeating the same snackbar repeatedly during recompositions
    var shownNoUpdateSnackbar by remember { mutableStateOf(false) }
    var shownUpdateSnackbar by remember { mutableStateOf(false) }
    var shownErrorSnackbar by remember { mutableStateOf(false) }

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
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Behaviour",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {

                    SettingTile(
                        title = "Show package names",
                        subtitle = "Display the package name beneath each app in the chooser",
                        checked = showPackageName,
                        onCheckedChange = { enabled ->
                            performHapticFeedback(context)
                            showPackageName = enabled
                            onToggleShowPackageName(enabled)
                        },
                        shape = RoundedCornerShape(
                            topStart = 24.dp, topEnd = 24.dp,
                            bottomStart = 8.dp, bottomEnd = 8.dp
                        )
                    )

                    SettingTile(
                        title = "Open App",
                        subtitle = "Clicking on app name opens the app instead of voice assistant",
                        checked = openApp,
                        onCheckedChange = { enabled ->
                            performHapticFeedback(context)
                            openApp = enabled
                            onToggleOpenApp(enabled)
                        },
                        shape = RoundedCornerShape(
                            topStart = 8.dp, topEnd = 8.dp,
                            bottomStart = 8.dp, bottomEnd = 8.dp
                        )
                    )

                    SettingTile(
                        title = "Close app after activity launch",
                        subtitle = "Close AssistantChooser after launching another app",
                        checked = closeAfter,
                        onCheckedChange = { enabled ->
                            performHapticFeedback(context)
                            closeAfter = enabled
                            onToggleCloseAfter(enabled)
                        },
                        shape = RoundedCornerShape(
                            topStart = 8.dp, topEnd = 8.dp,
                            bottomStart = 24.dp, bottomEnd = 24.dp
                        )
                    )
                }

            }

            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )

                val topShape = RoundedCornerShape(
                    topStart = 24.dp, topEnd = 24.dp,
                    bottomStart = 8.dp, bottomEnd = 8.dp
                )
                val middleShape = RoundedCornerShape(8.dp)
                val bottomShape = RoundedCornerShape(
                    topStart = 8.dp, topEnd = 8.dp,
                    bottomStart = 24.dp, bottomEnd = 24.dp
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ClickableTile(
                        title = "Ayaan Hussain",
                        subtitle = "Developer",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Ayaanh001")
                            )
                            context.startActivity(intent)
                        },
                        shape = topShape
                    )

                    ClickableTile(
                        title = "GitHub",
                        subtitle = "source code repository",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Ayaanh001/Assistant-Chooser.git")
                            )
                            context.startActivity(intent)
                        },
                        shape = middleShape
                    )

                    // VERSION ROW WITH CHECK UPDATES BUTTON
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = bottomShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Version",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentVersion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Check updates button (disabled while loading, shows spinner)
                            Button(
                                onClick = {
                                    // reset shown flags to allow new snackbars on each check
                                    shownNoUpdateSnackbar = false
                                    shownUpdateSnackbar = false
                                    shownErrorSnackbar = false

                                    loading = true
                                    // Launch coroutine to fetch latest version
                                    scope.launch(Dispatchers.IO) {
                                        val fetched = checkLatestVersionFromGitHub(
                                            "Ayaanh001",
                                            "Assistant-Chooser"
                                        )

                                        withContext(Dispatchers.Main) {
                                            if (fetched == null) {
                                                // network / error
                                                latestVersion = null
                                                updateAvailable = false
                                            } else {
                                                latestVersion = fetched.removePrefix("v")
                                                // use robust comparison
                                                updateAvailable = isNewerVersion(
                                                    latestVersion!!,
                                                    currentVersion
                                                )
                                            }
                                            loading = false

                                            // Show appropriate snackbar
                                            scope.launch {
                                                // Update available
                                                if (updateAvailable == true && !shownUpdateSnackbar) {
                                                    shownUpdateSnackbar = true
                                                    val message = "New update available: ${latestVersion ?: ""}"
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = message,
                                                        actionLabel = "Download",
                                                        duration = androidx.compose.material3.SnackbarDuration.Indefinite
                                                    )
                                                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                        // Open releases page
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse("https://github.com/Ayaanh001/Assistant-Chooser/releases/latest")
                                                        )
                                                        context.startActivity(intent)
                                                    }
                                                } else if (fetched == null && !shownErrorSnackbar) {
                                                    shownErrorSnackbar = true
                                                    snackbarHostState.showSnackbar(
                                                        message = "Failed to check updates",
                                                        actionLabel = "OK",
                                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                                    )
                                                } else if (updateAvailable == false && !shownNoUpdateSnackbar) {
                                                    shownNoUpdateSnackbar = true
                                                    snackbarHostState.showSnackbar(
                                                        message = "No updates available",
                                                        actionLabel = "OK",
                                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !loading,
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.wrapContentSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Check updates",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}


suspend fun checkLatestVersionFromGitHub(owner: String, repo: String): String? {
    return try {
        val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        try {
            val code = conn.responseCode
            if (code in 200..299) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val tag = JSONObject(json).optString("tag_name", null)
                tag
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
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

@Composable
fun ClickableTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
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
                .clickable { onClick() }
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
        }
    }
}