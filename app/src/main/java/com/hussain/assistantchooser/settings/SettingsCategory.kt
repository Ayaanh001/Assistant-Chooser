package com.hussain.assistantchooser.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hussain.assistantchooser.R

enum class SettingsCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val iconColor: Color
) {
    APPEARANCE(
        id = "appearance",
        titleRes = R.string.settings_category_appearance_title,
        subtitleRes = R.string.settings_category_appearance_subtitle,
        icon = Icons.Default.Palette,
        iconColor = Color(0xFF9C27B0)
    ),
    BEHAVIOUR(
        id = "behaviour",
        titleRes = R.string.settings_category_behaviour_title,
        subtitleRes = R.string.settings_category_behaviour_subtitle,
        icon = Icons.Default.Tune,
        iconColor = Color(0xFF009688)
    ),
    OVERLAY(
        id = "overlay",
        titleRes = R.string.settings_category_overlay_title,
        subtitleRes = R.string.settings_category_overlay_subtitle,
        icon = Icons.Default.Assistant,
        iconColor = Color(0xFF3F51B5)
    ),
    BACKUP_RESTORE(
        id = "backup_restore",
        titleRes = R.string.settings_category_backup_title,
        subtitleRes = R.string.settings_category_backup_subtitle,
        icon = Icons.Default.FileUpload,
        iconColor = Color(0xFF2196F3)
    ),
    ABOUT(
        id = "about",
        titleRes = R.string.settings_category_about_title,
        subtitleRes = R.string.settings_category_about_subtitle,
        icon = Icons.Default.Info,
        iconColor = Color(0xFFFF9800)
    );

    companion object {
        fun fromId(id: String): SettingsCategory? {
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
