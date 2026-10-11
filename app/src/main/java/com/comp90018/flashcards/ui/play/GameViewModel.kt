package com.comp90018.flashcards.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.local.entity.CardEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * The round that Duo and team mode share: one card at a time, a countdown on each front, then the players
 * mark the guess right or wrong. The subclasses decide where the cards come from and what happens
 * around the round (Duo can play again, team mode compares scores with the other team).
 *
 * Each card gets a countdown of [secondsPerCard]. When it runs out, or the Guesser acts first, the
 * back is revealed. [onGesture] is where the tilt sensor plugs in. Until then the screen sends it
 * from taps.
 */
abstract class GameViewModel(
    private val secondsPerCard: Int,
) : ViewModel() {
    private val _round = MutableStateFlow(DuoUiState(secondsPerCard = secondsPerCard))
    val round: StateFlow<DuoUiState> = _round.asStateFlow()

    private var game = DuoGame(emptyList())
    private var timerJob: Job? = null

    // while true the countdown holds still, used when the link to the other phone is down
    private val paused = MutableStateFlow(false)

    /** Sets up a new round on these cards, in the order given, and waits on the Ready screen. */
    protected fun loadCards(
        cards: List<CardEntity>,
        deckName: String,
    ) {
        timerJob?.cancel()
        game = DuoGame(cards)
        publish(isLoading = false, deckName = deckName)
    }

    /** Called once when the last card has been marked. */
    protected open fun onFinished(correctCount: Int) = Unit

    /** Holds the countdown still, or lets it carry on. */
    protected fun setPaused(isPaused: Boolean) {
        paused.value = isPaused
    }

    /** Starts the round, called from the Ready screen. */
    fun start() {
        game.start()
        if (game.phase == DuoPhase.FRONT) startTimer()
        publish()
    }

    /** Shows the back of the current card, ahead of the countdown. */
    fun reveal() {
        timerJob?.cancel()
        showBack()
    }

    /**
     * Handles a tilt. With the front showing, either direction reveals the back.
     * With the back showing, DOWN marks the guess right and UP marks it wrong.
     */
    fun onGesture(gesture: TiltGesture) {
        when (game.phase) {
            DuoPhase.FRONT -> reveal()
            DuoPhase.BACK -> mark(isCorrect = gesture == TiltGesture.DOWN)
            DuoPhase.READY, DuoPhase.FINISHED -> Unit
        }
    }

    private fun mark(isCorrect: Boolean) {
        game.mark(isCorrect)
        if (game.phase == DuoPhase.FRONT) startTimer()
        publish()
        if (game.phase == DuoPhase.FINISHED) onFinished(game.correctCount)
    }

    private fun showBack() {
        game.reveal()
        publish()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                var left = secondsPerCard
                while (left > 0) {
                    // wait here while paused, so the number on screen stops counting down
                    paused.first { !it }
                    _round.update { it.copy(secondsLeft = left) }
                    delay(1.seconds)
                    left--
                }
                showBack()
            }
    }

    private fun publish(
        isLoading: Boolean = false,
        deckName: String = _round.value.deckName,
    ) {
        _round.update {
            it.copy(
                isLoading = isLoading,
                deckName = deckName,
                phase = game.phase,
                current = game.current,
                correctCount = game.correctCount,
                totalCount = game.totalCount,
                missed = game.missed,
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
    }
}
