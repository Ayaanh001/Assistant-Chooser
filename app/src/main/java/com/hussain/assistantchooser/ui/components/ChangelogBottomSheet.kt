package com.hussain.assistantchooser.ui.components

import com.hussain.assistantchooser.settings.GitHubRelease
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ACChangelogSection(
    val title: String,
    val items: List<String>
)

data class ACChangelogVersion(
    val version: String,
    val date: String,
    val sections: List<ACChangelogSection>
)

private val assistantChooserChangelog = listOf(

    ACChangelogVersion(
        version = "v1.4",
        date = "19-08-2026",
        sections = listOf(
            ACChangelogSection(
                title = "Added",
                items = listOf(
                    "Backup feature to export/import custom apps list and app settings",
                    "Themed icons support for main app and overlay",
                    "Option to open overlay using Quick Settings tile",
                    "Empty screen UI for custom apps filter",
                )
            ),
            ACChangelogSection(
                title = "Changed & Improved",
                items = listOf(
                    "Refactored Settings screen UI for a cleaner look",
                    "Improved Overlay UI and animations",
                    "Updated app color palette",
                    "Improved 'Choose Apps' picker bottom sheet UI",
                    "Polished GroupSurface logic and skeleton loading placeholders"
                )
            ),
            ACChangelogSection(
                title = "Fixed",
                items = listOf(
                    "Fixed overlay launching behavior to respect 'Open App' setting",
                    "Real-time detection and refresh of newly installed apps"
                )
            )
        )
    ),

    ACChangelogVersion(
        version = "v1.3",
        date = "02-03-2026",
        sections = listOf(
            ACChangelogSection(
                title = "Added",
                items = listOf(
                    "Overlay feature when accessed through assistant launching gestures",
                    "Overlay app source setting — choose assistant apps or custom list",
                    "Show app names toggle for overlay grid",
                    "Skeleton loading screen with shimmer placeholders"
                )
            ),
            ACChangelogSection(
                title = "Changed",
                items = listOf(
                    "Full project restructure",
                    "R8 minification & resource shrinking (reduced app size)",
                )
            ),
            ACChangelogSection(
                title = "Improvements",
                items = listOf(
                    "Reduced the app launching speed by 50%",
                    "Made the apps list loading much faster"
                )
            ),
        )
    ),

    ACChangelogVersion(
        version = "v1.2",
        date = "11-12-2025",
        sections = listOf(
            ACChangelogSection(
                title = "Added",
                items = listOf(
                    "Filter button — switch between Assistant Apps & Custom Apps",
                    "Show / Hide package name option in Settings",
                    "Check for updates button (GitHub latest release)",
                    "Predictive back gesture support"
                )
            ),
            ACChangelogSection(
                title = "Fixed",
                items = listOf(
                    "Radio button now syncs correctly with default assistant"
                )
            )
        )
    ),

    ACChangelogVersion(
        version = "v1.1",
        date = "07-12-2025",
        sections = listOf(
            ACChangelogSection(
                title = "Added",
                items = listOf(
                    "Add Quick Settings Tile",
                    "Open App setting",
                    "Auto-close after launch setting",
                    "About section with developer info and source link"
                )
            ),
            ACChangelogSection(
                title = "Fixed",
                items = listOf(
                    "Gemini Assistant fallback to Google Voice Search"
                )
            )
        )
    ),

    ACChangelogVersion(
        version = "v1.0",
        date = "16-10-2024",
        sections = listOf(
            ACChangelogSection(
                title = "Added",
                items = listOf(
                    "Initial public release",
                    "Launch all installed assistant & voice search apps",
                    "Navigate to system default assistant settings",
                    "Material You (Material 3) UI with dynamic color"
                )
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChangelogBottomSheet(
    updateRelease: GitHubRelease? = null,
    onDownloadClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var showFullHistory by remember { mutableStateOf(updateRelease == null) }
    val expandedVersions = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {

        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = if (updateRelease != null) 140.dp else 80.dp
                )
            ) {

                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(top = 16.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (updateRelease != null) "Update Available" else "Changelog",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (updateRelease != null && updateRelease.body.isNotBlank()) {
                    item {
                        UpdateNotesCard(updateRelease.body)
                    }

                    item {
                        TextButton(
                            onClick = { showFullHistory = !showFullHistory },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showFullHistory) "Hide full version history" 
                                else "View full version history"
                            )
                        }
                    }
                }

                if (showFullHistory) {
                    items(assistantChooserChangelog) { version ->
                        VersionItem(
                            version = version,
                            isExpanded = expandedVersions[version.version] == true,
                            onToggleExpand = {
                                val current = expandedVersions[version.version] ?: false
                                expandedVersions[version.version] = !current
                            }
                        )
                    }
                }
            }

            if (updateRelease != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Download Latest Version",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateNotesCard(notes: String) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "What's New",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun VersionItem(
    version: ACChangelogVersion,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onToggleExpand() }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = version.version,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = version.date,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                version.sections.forEach { section ->
                    SectionCard(section)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(section: ACChangelogSection) {

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            section.items.forEachIndexed { index, item ->

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(7.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )

                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (index != section.items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}