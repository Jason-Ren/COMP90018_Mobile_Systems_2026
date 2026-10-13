package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.vibration.GameVibration

// records which buzzes were asked for, true for right and false for wrong, how many reveals and how many times up
class FakeGameVibration : GameVibration {
    val buzzed = mutableListOf<Boolean>()
    var reveals = 0
    var timeUps = 0

    override fun vibrate(isCorrect: Boolean) {
        buzzed += isCorrect
    }

    override fun vibrateReveal() {
        reveals++
    }

    override fun vibrateTimeUp() {
        timeUps++
    }
}
