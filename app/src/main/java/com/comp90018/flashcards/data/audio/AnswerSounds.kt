package com.comp90018.flashcards.data.audio

// plays the right and wrong sounds, behind an interface so view models can be tested without audio
interface AnswerSounds {
    fun play(isCorrect: Boolean)
}
