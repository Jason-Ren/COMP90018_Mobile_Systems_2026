package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.audio.AnswerSounds

// records which sounds were asked for, true for right and false for wrong
class FakeAnswerSounds : AnswerSounds {
    val played = mutableListOf<Boolean>()

    override fun play(isCorrect: Boolean) {
        played += isCorrect
    }
}
