package com.comp90018.flashcards.ui.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Calls [onPause] whenever the app leaves the foreground, for example when the player switches
 * apps or the screen turns off. The game screens use it to stop their countdown.
 */
@Composable
fun PauseWhenBackgrounded(onPause: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPause by rememberUpdatedState(onPause)
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) currentOnPause()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
