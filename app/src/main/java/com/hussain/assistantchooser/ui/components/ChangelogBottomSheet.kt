package com.hussain.assistantchooser.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDismiss: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp,
        dragHandle = null
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 80.dp
            )
        ) {

            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .statusBarsPadding()
                        .padding(top = 12.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Changelog",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(assistantChooserChangelog) { version ->
                VersionItem(version)
            }
        }
    }
}

@Composable
private fun VersionItem(version: ACChangelogVersion) {

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = version.version,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = version.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        version.sections.forEach { section ->
            SectionCard(section)
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