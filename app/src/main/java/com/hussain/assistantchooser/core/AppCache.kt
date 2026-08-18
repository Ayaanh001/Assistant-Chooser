package com.hussain.assistantchooser.core

import com.hussain.assistantchooser.settings.GitHubRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppCache {

    data class CacheState(
        val assistantApps: List<AssistantApp> = emptyList(),
        val allApps: List<AssistantApp>       = emptyList(),
        val isReady: Boolean                  = false,
        val latestRelease: GitHubRelease?     = null
    )

    private val _state = MutableStateFlow(CacheState())

    /** Observe this to react when the cache is populated. */
    val state: StateFlow<CacheState> = _state.asStateFlow()

    /** Convenience accessors (safe to call anytime; empty until ready). */
    val assistantApps: List<AssistantApp> get() = _state.value.assistantApps
    val allApps: List<AssistantApp>       get() = _state.value.allApps
    val isReady: Boolean                  get() = _state.value.isReady
    val latestRelease: GitHubRelease?     get() = _state.value.latestRelease

    /** Called from Application#onCreate on a background thread. */
    fun populate(assistantApps: List<AssistantApp>, allApps: List<AssistantApp>) {
        _state.value = _state.value.copy(
            assistantApps = assistantApps,
            allApps       = allApps,
            isReady       = true
        )
    }

    fun setLatestRelease(release: GitHubRelease?) {
        _state.value = _state.value.copy(latestRelease = release)
    }
}
