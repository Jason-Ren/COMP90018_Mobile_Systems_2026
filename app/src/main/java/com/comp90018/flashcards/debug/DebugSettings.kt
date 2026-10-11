package com.comp90018.flashcards.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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
            _emulatorMode.value = enabled
        }
    }
