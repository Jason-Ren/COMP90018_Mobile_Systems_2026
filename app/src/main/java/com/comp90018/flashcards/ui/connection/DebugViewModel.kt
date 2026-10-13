package com.comp90018.flashcards.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.debug.DebugSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// DEBUG ONLY: gives the debug composables the emulator mode tickbox and the link simulations
@HiltViewModel
class DebugViewModel
    @Inject
    constructor(
        private val debugSettings: DebugSettings,
    ) : ViewModel() {
        val emulatorMode: StateFlow<Boolean> = debugSettings.emulatorMode

        fun dropConnection() {
            viewModelScope.launch { debugSettings.simulateDrop() }
        }

        fun peerLeft() = debugSettings.simulatePeerEnded()
    }
