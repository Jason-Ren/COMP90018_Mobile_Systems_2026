package com.comp90018.flashcards.ui.play

/**
 * The countdown for one card, kept free of coroutines so it is easy to test.
 *
 * The ViewModel calls [tick] once a second. While the timer is paused, ticks are ignored and the
 * time left is kept, so resuming carries on from where it stopped.
 */
class CardTimer(
    private val totalSeconds: Int,
) {
    init {
        require(totalSeconds > 0) { "totalSeconds must be positive, got $totalSeconds" }
    }

    var secondsLeft: Int = totalSeconds
        private set

    var isPaused: Boolean = false
        private set

    val isFinished: Boolean
        get() = secondsLeft <= 0

    /** Starts over with the full time, unpaused. */
    fun reset() {
        secondsLeft = totalSeconds
        isPaused = false
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    /** One second passes. Returns true if that used up the last second. Does nothing while paused. */
    fun tick(): Boolean {
        if (isPaused || isFinished) return false
        secondsLeft--
        return isFinished
    }
}
