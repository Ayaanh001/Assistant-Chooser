package com.hussain.assistantchooser

import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    // SharedPreferences keys
    private val PREFS_NAME = "assistant_prefs"
    private val KEY_OPEN_APP = "open_app"
    private val KEY_CLOSE_AFTER_LAUNCH = "close_after_launch"
    private val openAppState = mutableStateOf(false)
    private val closeAfterLaunchState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // load initial preferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        openAppState.value = prefs.getBoolean(KEY_OPEN_APP, false)
        closeAfterLaunchState.value = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)

        val pm = packageManager

        // Query for apps responding to ACTION_ASSIST (activities)
        val assistIntent = Intent(Intent.ACTION_ASSIST)
        val assistResolveInfos = pm.queryIntentActivities(assistIntent, PackageManager.MATCH_ALL)

        // Query for services implementing VoiceInteractionService
        val voiceIntent = Intent("android.service.voice.VoiceInteractionService")
        val voiceResolveInfos = pm.queryIntentServices(voiceIntent, PackageManager.MATCH_ALL)

        // Build list of AssistantApp objects from activities
        val assistApps = assistResolveInfos.map {
            val appInfo = it.activityInfo.applicationInfo
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AssistantApp(label, appInfo.packageName, icon)
        }

        // Build list of AssistantApp objects from services
        val voiceApps = voiceResolveInfos.map {
            val appInfo = it.serviceInfo.applicationInfo
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AssistantApp(label, appInfo.packageName, icon)
        }

        // Combine lists and remove duplicates by package name
        val combinedApps = (assistApps + voiceApps)
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }

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

                var selectedPackage by remember { mutableStateOf<String?>(null) }
                val context = LocalContext.current

                Surface(modifier = Modifier.fillMaxSize()) {
                    AssistantChooserScreen(
                        assistantApps = combinedApps,
                        selectedPackage = selectedPackage,
                        onAppSelected = { selectedApp ->
                            selectedPackage = selectedApp.packageName

                            // Open assistant settings to let user set default - keep original behaviour
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
                            // Use current settings to decide behavior
                            if (openAppState.value) {
                                // Open *app* (launch intent) first
                                try {
                                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    } else {
                                        // fallback to assistant behavior if no launchable activity
                                        launchAssistantForPackage(context, pm, pkg)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error launching app for $pkg", e)
                                    launchAssistantForPackage(context, pm, pkg)
                                }
                            } else {
                                // Open assistant behavior (voice/assist) as before
                                launchAssistantForPackage(context, pm, pkg)
                            }

                            // If setting enabled, close this activity
                            if (closeAfterLaunchState.value) {
                                finish()
                            }
                        },
                        onSettingsClick = {
                            // Open in-app SettingsActivity
                            val intent = Intent(context,
                                SettingsActivity::class.java).apply{
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent) },

                        onAddTileClicked = {
                            // Request add quick settings tile (handles API 33+ and fallback)
                            requestAddQuickSettingsTile()
                        },
                        openApp = openAppState.value,
                        closeAfterLaunch = closeAfterLaunchState.value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // reload settings in case user changed them in SettingsActivity
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        openAppState.value = prefs.getBoolean(KEY_OPEN_APP, false)
        closeAfterLaunchState.value = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
    }

    private fun requestAddQuickSettingsTile() {
        val context = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
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

            val executor = Executors.newSingleThreadExecutor()

            try {
                statusBar.requestAddTileService(
                    tileComponent,
                    tileLabel,
                    tileIcon,
                    executor,
                    java.util.function.Consumer { result ->
                        // result may be Boolean, java.lang.Boolean, Int, etc. Normalize it safely:
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

    private fun launchAssistantForPackage(context: android.content.Context, pm: PackageManager, pkg: String) {
        try {
            // 1. Google / Gemini Override
            if (pkg == "com.google.android.googlequicksearchbox") {
                val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    Log.d("MainActivity", "Forced ACTION_VOICE_COMMAND for Google")
                    return
                } catch (e: Exception) {
                    Log.w("MainActivity", "Force launch failed for Google, falling back...")
                }
            }

            // 2. ChatGPT Override
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
                    // Fall through to generic launcher
                }
            }

            // 3) ACTION_ASSIST -> find an activity that declared this action
            val assistResolvers = packageManager.queryIntentActivities(Intent(Intent.ACTION_ASSIST), PackageManager.MATCH_ALL)
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

            // 4) ACTION_VOICE_COMMAND (Generic)
            val voiceCmdResolvers = packageManager.queryIntentActivities(Intent(Intent.ACTION_VOICE_COMMAND), PackageManager.MATCH_ALL)
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

            // 5) Fallback to Main Launch Intent
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "No launchable activity found", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching assistant for $pkg", e)
            // Final fallback
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
        }
    }
}

data class AssistantApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChooserScreen(
    assistantApps: List<AssistantApp>,
    selectedPackage: String?,
    onAppSelected: (AssistantApp) -> Unit,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onAddTileClicked: () -> Unit,
    openApp: Boolean,
    closeAfterLaunch: Boolean,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    actions = {
                        var menuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Tile") },
                                onClick = {
                                    menuExpanded = false
                                    onAddTileClicked()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            )
                        }
                    },
                    title = {
                        Text(
                            "Assistant Chooser",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(assistantApps) { index, app ->
                    val shape = when {
                        assistantApps.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(
                            topStart = 24.dp, topEnd = 24.dp,
                            bottomStart = 8.dp, bottomEnd = 8.dp
                        )
                        index == assistantApps.size - 1 -> RoundedCornerShape(
                            topStart = 8.dp, topEnd = 8.dp,
                            bottomStart = 24.dp, bottomEnd = 24.dp
                        )
                        else -> RoundedCornerShape(8.dp)
                    }

                    AssistantAppRadioCard(
                        app = app,
                        shape = shape,
                        selected = app.packageName == selectedPackage,
                        onSelect = { onAppSelected(app) },
                        onOpenApp = { onAppClick(app.packageName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Open Digital Assistant Settings
                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Open Default Assistant Settings")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AssistantAppRadioCard(
    app: AssistantApp,
    shape: RoundedCornerShape,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenApp: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Left side clickable: open app / assistant depending on setting
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenApp() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Radio button on right side: selecting default assistant
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
        }
    }
}

// Drawable → Bitmap helper
fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return this.bitmap
    val width = intrinsicWidth.takeIf { it > 0 } ?: 1
    val height = intrinsicHeight.takeIf { it > 0 } ?: 1
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}