package com.comp90018.flashcards.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG ONLY: set this to false to hide every emulator mode feature at once. The tickbox on the main
 * screen disappears and the emulator mode stays off, so the app behaves like the real thing.
 */
const val DEBUG_TOOLS_ENABLED = true

/**
 * DEBUG ONLY: switches for trying things out without a second phone. In memory, so they reset
 * when the app restarts.
 */
@Singleton
class DebugSettings
    @Inject
    constructor() {
        private val _emulatorMode = MutableStateFlow(false)

        // on an emulator nearby cannot connect, so the lobby gets a fake join request button
        val emulatorMode: StateFlow<Boolean> = _emulatorMode.asStateFlow()

        fun setEmulatorMode(enabled: Boolean) {
            _emulatorMode.value = enabled && DEBUG_TOOLS_ENABLED
        }
    }
