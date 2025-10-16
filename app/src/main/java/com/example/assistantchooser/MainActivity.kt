package com.hussain.assistantchooser

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
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
import com.hussain.assistantchooser.ui.components.GroupSurface
import com.hussain.assistantchooser.ui.theme.AssistantChooserTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

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

                            // Open assistant settings to let user set default
                            val settingsIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            context.startActivity(settingsIntent)

                            Toast.makeText(
                                context,
                                "Select \"${selectedApp.name}\" as your default assistant",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onAppClick = { pkg ->
                            val launchIntent = pm.getLaunchIntentForPackage(pkg)
                            launchIntent?.let { startActivity(it) }
                        },
                        onSettingsClick = {
                            val settingsIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            context.startActivity(settingsIntent)
                        }
                    )
                }
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
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
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
            // Use LazyColumn instead of Column for scrolling
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
                onClick = onSettingsClick,
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
            // Left side clickable: open app
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