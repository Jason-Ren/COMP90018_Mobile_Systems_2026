package com.comp90018.flashcards.data.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// one shared vibrator for the whole app, with a different pattern for right, wrong, reveal and time up
@Singleton
class SystemGameVibration
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : GameVibration {
        private val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

        override fun vibrate(isCorrect: Boolean) {
            // phones and emulators without a vibrator just do nothing
            if (vibrator?.hasVibrator() != true) return
            val effect = if (isCorrect) RIGHT else WRONG
            vibrator.vibrate(effect)
        }

        override fun vibrateReveal() {
            if (vibrator?.hasVibrator() != true) return
            vibrator.vibrate(REVEAL)
        }

        override fun vibrateTimeUp() {
            if (vibrator?.hasVibrator() != true) return
            vibrator.vibrate(TIME_UP)
        }

        private companion object {
            val RIGHT: VibrationEffect = VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)

            // lighter than the right tap, so a reveal does not feel like a verdict
            val REVEAL: VibrationEffect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)

            // the longest buzz, so running out of time cannot be mistaken for a tap
            val TIME_UP: VibrationEffect = VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)

            // wait, buzz, pause, buzz
            val WRONG: VibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 70), -1)
        }
    }
