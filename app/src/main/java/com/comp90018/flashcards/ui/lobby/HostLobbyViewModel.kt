package com.comp90018.flashcards.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.p2p.ConnectionController
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.debug.DebugSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What is in the opponent's half of the lobby. */
sealed interface GuestSlot {
    /** Nobody has asked to join yet. */
    data object Empty : GuestSlot

    /** A team has asked to join and the host has not answered. */
    data class Pending(
        val teamName: String,
    ) : GuestSlot

    /** The host accepted this team. */
    data class Joined(
        val teamName: String,
    ) : GuestSlot
}

data class LobbyUiState(
    val teamName: String,
    val guest: GuestSlot = GuestSlot.Empty,
    // raw link state, the screen uses it for the reconnecting and lost connection screens
    val connection: ConnectionState = ConnectionState.Idle,
    // DEBUG ONLY: true when emulator mode is ticked on the main screen, shows the fake join request button
    val showDebugButton: Boolean = false,
) {
    // Later: also require that the deck has finished syncing to the guest.
    val canStart: Boolean get() = guest is GuestSlot.Joined

    // allow rerolling before anyone joins OR when guest is connected in lobby (not when pending request)
    val canReroll: Boolean get() = guest is GuestSlot.Empty || guest is GuestSlot.Joined
}

/**
 * ViewModel for the host's team mode waiting lobby.
 *
 * The connection itself lives in [ConnectionController]. This manages advertising host presence,
 * team names, and handling guest join requests.
 */
@HiltViewModel
class HostLobbyViewModel
    @Inject
    constructor(
        private val connection: ConnectionController,
        private val debugSettings: DebugSettings,
    ) : ViewModel() {
        private val teamName = MutableStateFlow(randomTeamName())

        // the controller is app wide, so ignore its state until this lobby has started hosting
        private val started = MutableStateFlow(false)

        // DEBUG ONLY: a fake guest so the lobby can be tried on an emulator, where nearby cannot connect
        private val debugGuest = MutableStateFlow<GuestSlot?>(null)

        // guest's team name updated via network message
        private val guestNameOverride = MutableStateFlow<String?>(null)

        // in emulator mode nearby is never started, so the real link is ignored and the simulated one is used
        private val link =
            combine(
                connection.state,
                started,
                debugSettings.emulatorMode,
                debugSettings.simulatedLink,
            ) { real, hasStarted, emulator, simulated ->
                when {
                    emulator -> simulated ?: ConnectionState.Idle
                    hasStarted -> real
                    else -> ConnectionState.Idle
                }
            }

        val state: StateFlow<LobbyUiState> =
            combine(
                teamName,
                link,
                debugGuest,
                guestNameOverride,
                debugSettings.emulatorMode,
            ) { name, current, fake, guestOverride, emulator ->
                val resolvedGuest = fake ?: guestSlotOf(current)
                val finalGuest =
                    if (resolvedGuest is GuestSlot.Joined && guestOverride != null) {
                        GuestSlot.Joined(guestOverride)
                    } else {
                        resolvedGuest
                    }
                LobbyUiState(
                    teamName = name,
                    guest = finalGuest,
                    connection = current,
                    showDebugButton = emulator,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LobbyUiState(teamName.value))

        init {
            viewModelScope.launch {
                connection.messages.collect { msg ->
                    if (msg is GameMessage.UpdateTeamName) {
                        guestNameOverride.value = msg.teamName
                    }
                }
            }
        }

        // called once permissions are granted, and only once so rotating does not drop the guest
        fun begin() {
            if (started.value) return
            started.value = true
            if (!debugSettings.emulatorMode.value) connection.startHosting(teamName.value)
        }

        fun rerollName() {
            if (!state.value.canReroll) return
            teamName.value = randomTeamName()
            if (connection.state.value is ConnectionState.Advertising) {
                connection.startHosting(teamName.value)
            } else if (state.value.guest is GuestSlot.Joined) {
                if (debugGuest.value == null) {
                    connection.send(GameMessage.UpdateTeamName(teamName.value))
                }
            }
        }

        fun accept() {
            val fake = debugGuest.value
            if (fake is GuestSlot.Pending) debugGuest.value = GuestSlot.Joined(fake.teamName) else connection.accept()
        }

        fun decline() {
            if (debugGuest.value != null) debugGuest.value = null else connection.reject()
        }

        fun removeGuest() {
            if (debugGuest.value != null) debugGuest.value = null else connection.removePeer()
        }

        fun reconnect() {
            if (debugSettings.emulatorMode.value) {
                viewModelScope.launch { debugSettings.simulateReconnect() }
            } else {
                connection.reconnect()
            }
        }

        fun cancel() {
            debugGuest.value = null
            debugSettings.clearSimulatedLink()
            connection.endSession()
        }

        // DEBUG ONLY: pretend a team asked to join
        fun debugSimulateRequest() {
            debugGuest.value = GuestSlot.Pending("Team Cool Kiwi")
        }
    }

private const val STOP_TIMEOUT_MS = 5_000L

// a dropped guest stays in the slot while we try to get them back
private fun guestSlotOf(link: ConnectionState): GuestSlot =
    when (link) {
        is ConnectionState.Requested -> GuestSlot.Pending(link.peerName)
        is ConnectionState.Connected -> GuestSlot.Joined(link.peerName)
        is ConnectionState.Reconnecting -> GuestSlot.Joined(link.peerName)
        is ConnectionState.Lost -> GuestSlot.Joined(link.peerName)
        else -> GuestSlot.Empty
    }
