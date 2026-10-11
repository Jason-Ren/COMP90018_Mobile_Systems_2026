package com.comp90018.flashcards.ui.play

import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.p2p.ConnectionController
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.GameSession
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
import kotlin.random.Random

/** One phone's view of a team mode game: its own round, the other team's score once known, and the link. */
data class TeamGameUiState(
    val round: DuoUiState = DuoUiState(),
    // null until the other team has finished its deck
    val opponentCorrect: Int? = null,
    val connection: ConnectionState = ConnectionState.Idle,
)

/**
 * ViewModel for a team mode game. Each team plays its own round exactly like Duo (the round is in
 * [GameViewModel]) on its own order of the deck, and the two scores are compared at the end.
 */
@HiltViewModel
class TeamGameViewModel
    @Inject
    constructor(
        private val connection: ConnectionController,
        session: GameSession,
        private val debugSettings: DebugSettings,
    ) : GameViewModel(secondsPerCard = session.setup?.secondsPerCard ?: 0) {
        private val setup = session.setup

        // false when the app was restarted mid game and the setup is gone, the screen then leaves
        val hasGame: Boolean = setup != null

        private val opponentCorrect = MutableStateFlow<Int?>(null)

        // in emulator mode nearby is never started, so there is no link to wait for
        private val link =
            combine(connection.state, debugSettings.emulatorMode) { real, emulator ->
                if (emulator) ConnectionState.Idle else real
            }

        val uiState: StateFlow<TeamGameUiState> =
            combine(round, opponentCorrect, link) { round, opponent, link ->
                TeamGameUiState(round, opponent, link)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, TeamGameUiState(round.value))

        private var finishedScore: Int? = null

        init {
            loadCards(setup?.cards.orEmpty(), setup?.deckName.orEmpty())
            viewModelScope.launch {
                connection.messages.collect { if (it is GameMessage.Finished) opponentCorrect.value = it.correct }
            }
            viewModelScope.launch {
                link.collect {
                    // the countdown waits while the link is down (but not in emulator mode, where there is none)
                    val linkDown = it !is ConnectionState.Connected && it !is ConnectionState.Idle
                    setPaused(linkDown)
                    // the score is lost if it was sent while the link was down, so send it again once we are back
                    if (it is ConnectionState.Connected) sendScore()
                }
            }
        }

        fun reconnect() = connection.reconnect()

        fun leave() = connection.endSession()

        override fun onFinished(correctCount: Int) {
            finishedScore = correctCount
            sendScore()
            // DEBUG ONLY: there is no other phone in emulator mode, so make up its score
            if (debugSettings.emulatorMode.value) {
                viewModelScope.launch {
                    delay(DEBUG_OPPONENT_MS)
                    opponentCorrect.value = Random.nextInt(round.value.totalCount + 1)
                }
            }
        }

        private fun sendScore() {
            finishedScore?.let { connection.send(GameMessage.Finished(it)) }
        }
    }

private const val DEBUG_OPPONENT_MS = 1_500L
