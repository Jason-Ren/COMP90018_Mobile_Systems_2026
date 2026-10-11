package com.comp90018.flashcards.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.p2p.ConnectionController
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.DiscoveredHost
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.GameSession
import com.comp90018.flashcards.data.p2p.GameSetup
import com.comp90018.flashcards.data.p2p.toSetup
import com.comp90018.flashcards.debug.DebugSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinUiState(
    val teamName: String,
    val hosts: List<DiscoveredHost> = emptyList(),
    val connection: ConnectionState = ConnectionState.Idle,
    // the host pressed Start
    val hostStarted: Boolean = false,
)

/** ViewModel for the guest's join screen: find hosts, ask to join, then wait for Start. */
@HiltViewModel
class JoinGameViewModel
    @Inject
    constructor(
        private val connection: ConnectionController,
        private val debugSettings: DebugSettings,
        private val session: GameSession,
    ) : ViewModel() {
        private val teamName = MutableStateFlow(randomTeamName())

        // the controller is app wide, so ignore its state until this screen has started searching
        private val started = MutableStateFlow(false)
        private val hostStarted = MutableStateFlow(false)

        // DEBUG ONLY: what the fake host has got up to, used in emulator mode instead of the real link
        private val debugLink = MutableStateFlow<ConnectionState?>(null)

        // in emulator mode nearby is never started, so the real link is ignored and the fake one is used
        private val link =
            combine(
                connection.state,
                started,
                debugSettings.emulatorMode,
                debugLink,
            ) { real, hasStarted, emulator, fake ->
                when {
                    emulator -> fake ?: ConnectionState.Idle
                    hasStarted -> real
                    else -> ConnectionState.Idle
                }
            }

        // DEBUG ONLY: a fake host in emulator mode
        private val hosts =
            combine(connection.hosts, debugSettings.emulatorMode) { real, emulator ->
                if (emulator) listOf(DEBUG_HOST) else real
            }

        val state: StateFlow<JoinUiState> =
            combine(teamName, hosts, link, hostStarted) { name, hosts, link, go ->
                JoinUiState(teamName = name, hosts = hosts, connection = link, hostStarted = go)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), JoinUiState(teamName.value))

        init {
            viewModelScope.launch {
                connection.messages.collect {
                    if (it is GameMessage.Start) startGame(it.toSetup())
                }
            }
        }

        // called once permissions are granted, and only once so rotating does not restart the search
        fun begin() {
            if (started.value) return
            started.value = true
            // in emulator mode nearby is never started
            if (!debugSettings.emulatorMode.value) connection.startJoining(teamName.value)
        }

        fun join(host: DiscoveredHost) {
            if (host == DEBUG_HOST) simulateJoin(host) else connection.requestToJoin(host)
        }

        fun reconnect() = connection.reconnect()

        fun leave() {
            debugLink.value = null
            hostStarted.value = false
            connection.endSession()
        }

        // keep the setup where the game screen can find it, then tell the screen to open the game
        private fun startGame(setup: GameSetup) {
            session.setup = setup
            hostStarted.value = true
        }

        // DEBUG ONLY: the fake host accepts after a moment, then starts the game a bit later
        private fun simulateJoin(host: DiscoveredHost) {
            viewModelScope.launch {
                debugLink.value = ConnectionState.Requested(host.teamName)
                delay(DEBUG_ACCEPT_MS)
                debugLink.value = ConnectionState.Connected(host.teamName)
                delay(DEBUG_START_MS)
                startGame(DEBUG_SETUP)
            }
        }
    }

// DEBUG ONLY: the fake host shown in emulator mode
private val DEBUG_HOST = DiscoveredHost(endpointId = "debug-host", teamName = "Team Plum")

// DEBUG ONLY: the cards the fake host starts the game with
private val DEBUG_SETUP =
    GameSetup(
        deckName = "Debug deck",
        secondsPerCard = 10,
        cards =
            List(
                5,
            ) { CardEntity(cardId = "debug-$it", deckId = "", front = "Card ${it + 1}", back = "Answer ${it + 1}") },
    )
private const val DEBUG_ACCEPT_MS = 1_500L
private const val DEBUG_START_MS = 3_000L

private const val STOP_TIMEOUT_MS = 5_000L
