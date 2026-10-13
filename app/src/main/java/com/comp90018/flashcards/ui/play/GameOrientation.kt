package com.comp90018.flashcards.ui.play

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Keeps the screen in landscape while [landscape] is true, and portrait otherwise.
 *
 * Used by the 2-player and 4-player modes, where the phone is held up to the forehead during play. Either
 * landscape direction is allowed, so the phone can be held either way round. The rest of the app is portrait.
 */
@Composable
fun GameOrientation(landscape: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, landscape) {
        // follow the current phase of the game
        context.findActivity()?.requestedOrientation = orientationFor(landscape)
        onDispose {}
    }
    // leaving the game screen, by back or a dropped connection, always goes back to portrait
    DisposableEffect(context) {
        onDispose { context.findActivity()?.requestedOrientation = orientationFor(false) }
    }
}

/**
 * Stops the screen dimming or locking while [enabled] is true.
 *
 * Nobody touches the phone during play, so without this it would dim mid-round.
 */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, enabled) {
        val window = context.findActivity()?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // always cleared on leave, so the rest of the app can dim normally
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

// either landscape direction, so the phone can be held either way round
private fun orientationFor(landscape: Boolean): Int =
    if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
