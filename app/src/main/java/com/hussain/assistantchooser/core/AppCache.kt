package com.hussain.assistantchooser.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppCache {

    data class CacheState(
        val assistantApps: List<AssistantApp> = emptyList(),
        val allApps: List<AssistantApp>       = emptyList(),
        val isReady: Boolean                  = false
    )

    private val _state = MutableStateFlow(CacheState())

    /** Observe this to react when the cache is populated. */
    val state: StateFlow<CacheState> = _state.asStateFlow()

    /** Convenience accessors (safe to call anytime; empty until ready). */
    val assistantApps: List<AssistantApp> get() = _state.value.assistantApps
    val allApps: List<AssistantApp>       get() = _state.value.allApps
    val isReady: Boolean                  get() = _state.value.isReady

    /** Called from Application#onCreate on a background thread. */
    fun populate(assistantApps: List<AssistantApp>, allApps: List<AssistantApp>) {
        _state.value = CacheState(
            assistantApps = assistantApps,
            allApps       = allApps,
            isReady       = true
        )
    }
}
