package com.comp90018.flashcards.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.audio.AnswerSounds
import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.vibration.GameVibration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * The round that Duo and clash mode share: one card at a time, a countdown on each front, then the players
 * mark the guess right or wrong. The subclasses decide where the cards come from and what happens
 * around the round (Duo can play again, clash mode compares scores with the other team).
 *
 * Each card gets a countdown of [secondsPerCard]. When it runs out, or the Guesser acts first, the
 * back is revealed. [onGesture] is where the tilt sensor plugs in. Until then the screen sends it
 * from taps.
 *
 * Marking a guess plays the right or wrong sound through [sounds] and buzzes through [vibration].
 * Revealing early and running out of time buzz too. Both modes get this from here.
 *
 * The countdown can be paused with [pause] (the screen does this when the app goes to the
 * background) and carries on from the same second after [resume].
 */
abstract class GameViewModel(
    private val secondsPerCard: Int,
    // plays the right and wrong sounds, shared by every game mode
    private val sounds: AnswerSounds,
    // buzzes the phone for a right or wrong guess, shared by every game mode
    private val vibration: GameVibration,
) : ViewModel() {
    private val _round = MutableStateFlow(DuoUiState(secondsPerCard = secondsPerCard))
    val round: StateFlow<DuoUiState> = _round.asStateFlow()

    private var game = DuoGame(emptyList())

    // coerced because a clash whose setup was lost has no countdown, and the timer needs one
    private val timer = CardTimer(secondsPerCard.coerceAtLeast(1))
    private var timerJob: Job? = null
    private var startCountdownJob: Job? = null

    // Independent holds: the player left the app, the link is down, or a pose-started phone was lowered.
    private var pausedByPlayer = false
    private var linkDown = false
    private var poseControlled = false
    private var poseMissing = false

    private val countdownHeld: Boolean
        get() = pausedByPlayer || linkDown || poseMissing

    /** Sets up a new round on these cards, in the order given, and waits on the Ready screen. */
    protected fun loadCards(
        cards: List<CardEntity>,
        deckName: String,
    ) {
        timerJob?.cancel()
        startCountdownJob?.cancel()
        timer.reset()
        pausedByPlayer = false
        poseControlled = false
        poseMissing = false
        _round.update { it.copy(startCountdown = 0) }
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

    /** Counts 3, 2, 1 on the screen and then starts the round, called from the Ready screen. */
    fun startWithCountdown() {
        if (_round.value.isLoading || game.phase != DuoPhase.READY || startCountdownJob?.isActive == true) return
        startCountdownJob =
            viewModelScope.launch {
                var number = START_COUNTDOWN_FROM
                while (number > 0) {
                    _round.update { it.copy(startCountdown = number) }
                    publish()
                    delay(1.seconds)
                    if (!countdownHeld) number--
                }
                _round.update { it.copy(startCountdown = 0) }
                start()
            }
    }

    /** A stable forehead pose starts a ready round; leaving it holds countdowns and scoring. */
    fun onForeheadPose(inPose: Boolean) {
        val state = _round.value
        if (state.isLoading || game.phase == DuoPhase.FINISHED) return
        val missing = !inPose
        if (inPose && game.phase == DuoPhase.READY && state.startCountdown == 0) {
            poseControlled = true
            poseMissing = false
            startWithCountdown()
        } else if (poseControlled && poseMissing != missing) {
            poseMissing = missing
            applyPause()
        }
    }

    /** Starts the round straight away. */
    fun start() {
        game.start()
        if (game.phase == DuoPhase.FRONT) startTimer()
        publish()
    }

    /** Shows the back of the current card, ahead of the countdown. Ignored while paused. */
    fun reveal() {
        if (_round.value.isPaused) return
        // a light tick, running out of time has its own longer buzz
        vibration.vibrateReveal()
        timerJob?.cancel()
        game.reveal()
        publish()
    }

    /** Holds the opening/card countdown, keeping the time left. The screen calls it in the background. */
    fun pause() {
        val canPause = game.phase == DuoPhase.FRONT || _round.value.startCountdown > 0
        if (!canPause || pausedByPlayer) return
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
        if (_round.value.isPaused) return
        when (game.phase) {
            DuoPhase.FRONT -> reveal()
            DuoPhase.BACK -> mark(isCorrect = gesture == TiltGesture.DOWN)
            DuoPhase.READY, DuoPhase.FINISHED -> Unit
        }
    }

    private fun mark(isCorrect: Boolean) {
        // sound and buzz first so they line up with the tap or tilt
        sounds.play(isCorrect)
        vibration.vibrate(isCorrect)
        game.mark(isCorrect)
        if (game.phase == DuoPhase.FRONT) startTimer()
        publish()
        if (game.phase == DuoPhase.FINISHED) onFinished(game.correctCount)
    }

    private fun startTimer() {
        timer.reset()
        // a card can come up while the link is down (marked from the back), so it starts held
        if (countdownHeld) timer.pause() else runTimer()
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
                vibration.vibrateTimeUp()
                game.reveal()
                publish()
            }
    }

    // Only fronts have a card timer; pose pauses also block scoring on the back.
    private fun applyPause() {
        if (game.phase == DuoPhase.FRONT) {
            val shouldHold = countdownHeld
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
                isPaused =
                    when {
                        game.phase == DuoPhase.FRONT -> timer.isPaused
                        it.startCountdown > 0 -> countdownHeld
                        game.phase == DuoPhase.BACK -> poseMissing
                        else -> false
                    },
                isPoseControlled = poseControlled,
                isPosePaused = poseMissing,
                correctCount = game.correctCount,
                totalCount = game.totalCount,
                missed = game.missed,
            )
        }
    }
}

// the number the countdown before a round starts from
private const val START_COUNTDOWN_FROM = 3
