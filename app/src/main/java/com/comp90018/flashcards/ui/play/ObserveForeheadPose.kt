package com.comp90018.flashcards.ui.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Listens only while the game screen is resumed; disposal/backgrounding clears the current pose. */
@Composable
internal fun observeForeheadPose(onPose: (Boolean) -> Unit): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentOnPose by rememberUpdatedState(onPose)
    val detector = remember(context) { ForeheadPoseDetector(context) { currentOnPose(it) } }
    // seed from sensor presence so the pose hint shows on the first frame
    var available by remember(detector) { mutableStateOf(detector.hasSensor) }
    DisposableEffect(detector, owner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> available = detector.start()
                    Lifecycle.Event.ON_PAUSE -> {
                        detector.stop()
                        currentOnPose(false)
                    }
                    else -> Unit
                }
            }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) available = detector.start()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            detector.stop()
            currentOnPose(false)
        }
    }
    return available
}
