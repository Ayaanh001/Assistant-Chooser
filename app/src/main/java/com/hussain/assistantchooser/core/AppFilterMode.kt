package com.hussain.assistantchooser.core

enum class AppFilterMode {
    VOICE_ASSISTANTS,
    CUSTOM_APPS
}

/** Which app list the overlay should render. Stored in SharedPreferences. */
enum class OverlaySource {
    ASSISTANT_APPS,
    CUSTOM_APPS;

    companion object {
        fun fromString(value: String?): OverlaySource =
            entries.firstOrNull { it.name == value } ?: ASSISTANT_APPS
    }
}
