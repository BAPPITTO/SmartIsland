/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.PrivacyIndicatorState
import com.agupta07505.smartisland.model.PrivacyKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds which privacy sources are in use right now (raw, before the 2 s / priority rules of
 * [com.agupta07505.smartisland.model.PrivacyIndicatorController]). Fed by [PrivacyIndicatorMonitor].
 */
class PrivacyIndicatorTracker {
    private val _state = MutableStateFlow(PrivacyIndicatorState())
    val state: StateFlow<PrivacyIndicatorState> = _state.asStateFlow()

    private val _holderPackage = MutableStateFlow<String?>(null)
    /** The app holding the screen projection, when the dump names it (best effort; null when unknown). */
    val holderPackage: StateFlow<String?> = _holderPackage.asStateFlow()

    fun setHolderPackage(pkg: String?) {
        _holderPackage.value = pkg
    }

    fun setActive(kind: PrivacyKind, active: Boolean) {
        val current = _state.value.active
        val next = if (active) current + kind else current - kind
        if (next == current) return
        _state.value = PrivacyIndicatorState(active = next)
    }

    fun setMediaProjectionActive(active: Boolean) = setActive(PrivacyKind.ScreenShare, active)

    /** Drops one source, e.g. when its switch is turned off. */
    fun clear(kind: PrivacyKind) {
        setActive(kind, false)
        if (kind == PrivacyKind.ScreenShare) setHolderPackage(null)
    }

    /** Drops everything, e.g. when the service stops. */
    fun clear() {
        _state.value = PrivacyIndicatorState()
        setHolderPackage(null)
    }
}
