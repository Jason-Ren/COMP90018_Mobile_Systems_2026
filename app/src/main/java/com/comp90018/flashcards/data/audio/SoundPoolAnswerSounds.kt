package com.comp90018.flashcards.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.comp90018.flashcards.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// one soundpool for the whole app, so both game modes share the two sounds loaded once
@Singleton
class SoundPoolAnswerSounds
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AnswerSounds {
        private val pool =
            SoundPool
                .Builder()
                .setMaxStreams(2)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).build()

        private val correctId = pool.load(context, R.raw.correct_choice, 1)
        private val wrongId = pool.load(context, R.raw.wrong_choice, 1)

        override fun play(isCorrect: Boolean) {
            // a sound that has not finished loading just stays silent
            pool.play(if (isCorrect) correctId else wrongId, 1f, 1f, 1, 0, 1f)
        }
    }
