package com.comp90018.flashcards.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.local.entity.CardEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 *
 * The countdown can be paused with [pause] (the screen does this when the app goes to the
 * background) and carries on from the same second after [resume].
 */
abstract class GameViewModel(
    private val secondsPerCard: Int,
) : ViewModel() {
    private val _round = MutableStateFlow(DuoUiState(secondsPerCard = secondsPerCard))
    val round: StateFlow<DuoUiState> = _round.asStateFlow()

    private var game = DuoGame(emptyList())

    // coerced because a team game whose setup was lost has no countdown, and the timer needs one
    private val timer = CardTimer(secondsPerCard.coerceAtLeast(1))
    private var timerJob: Job? = null

    // two reasons to hold the countdown: the player left the app, or the link to the other phone is down
    private var pausedByPlayer = false
    private var linkDown = false

    /** Sets up a new round on these cards, in the order given, and waits on the Ready screen. */
    protected fun loadCards(
        cards: List<CardEntity>,
        deckName: String,
    ) {
        timerJob?.cancel()
        timer.reset()
        pausedByPlayer = false
        game = DuoGame(cards)
        publish(isLoading = false, deckName = deckName)
    }

    /** Called once when the last card has been marked. */
    protected open fun onFinished(correctCount: Int) = Unit

    /** Holds the countdown while the link to the other phone is down, and lets it carry on when it is back. */
    protected fun setLinkDown(isDown: Boolean) {
        linkDown = isDown
        applyPause()
    }

    /** Starts the round, called from the Ready screen. */
    fun start() {
        game.start()
        if (game.phase == DuoPhase.FRONT) startTimer()
        publish()
    }

    /** Shows the back of the current card, ahead of the countdown. Ignored while paused. */
    fun reveal() {
        if (timer.isPaused) return
        timerJob?.cancel()
        showBack()
    }

    /** Stops the countdown while the front is showing, keeping the time left. The screen calls it in the background. */
    fun pause() {
        if (game.phase != DuoPhase.FRONT || pausedByPlayer) return
        pausedByPlayer = true
        applyPause()
    }

    /** Carries the countdown on from where [pause] stopped it. */
    fun resume() {
        pausedByPlayer = false
        applyPause()
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
        timer.reset()
        // a card can come up while the link is down (marked from the back), so it starts held
        if (linkDown) timer.pause() else runTimer()
    }

    /** Ticks the timer once a second until time is up, then shows the back. */
    private fun runTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (!timer.isFinished) {
                    delay(1.seconds)
                    timer.tick()
                    publish()
                }
                showBack()
            }
    }

    // the timer is held while either reason is on, and only a countdown on a front can be held
    private fun applyPause() {
        if (game.phase == DuoPhase.FRONT) {
            val shouldHold = pausedByPlayer || linkDown
            if (shouldHold && !timer.isPaused) {
                timerJob?.cancel()
                timer.pause()
            } else if (!shouldHold && timer.isPaused) {
                timer.resume()
                runTimer()
            }
        }
        publish()
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
                secondsLeft = timer.secondsLeft,
                isPaused = timer.isPaused,
                correctCount = game.correctCount,
                totalCount = game.totalCount,
                missed = game.missed,
            )
        }
    }
}
