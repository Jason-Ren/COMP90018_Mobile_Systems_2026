package com.comp90018.flashcards.data.vibration

// buzzes the phone for a right or wrong answer and a reveal or time up, behind an interface so tests need no vibrator
interface GameVibration {
    fun vibrate(isCorrect: Boolean)

    fun vibrateReveal()

    fun vibrateTimeUp()
}
