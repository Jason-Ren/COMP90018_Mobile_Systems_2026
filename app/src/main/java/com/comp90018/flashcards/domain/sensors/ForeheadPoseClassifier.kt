package com.comp90018.flashcards.domain.sensors

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Recognizes the upright landscape orientation used at the forehead, in device coordinates.
 * X runs along the short edge, Y along the long edge and Z out of the screen. Either landscape
 * direction is accepted. Gravity describes orientation, not the phone's height or proximity to a head.
 * This pure classifier can consume gravity from any sensor fusion implementation.
 */
class ForeheadPoseClassifier {
    var isInPose: Boolean = false
        private set

    private var candidateSinceNs: Long? = null
    private var lastTimestampNs: Long? = null

    /** Entry must hold for 600 ms; exit for 900 ms so brief scoring tilts do not pause the round. */
    fun update(
        x: Float,
        y: Float,
        z: Float,
        timestampNs: Long,
    ): Boolean {
        val previous = lastTimestampNs
        if (previous != null && timestampNs <= previous) return isInPose
        if (previous != null && timestampNs - previous > MAX_SAMPLE_GAP_NS) candidateSinceNs = null
        lastTimestampNs = timestampNs

        val magnitude = sqrt(x * x + y * y + z * z)
        val xLimit = if (isInPose) HOLD_X_MIN else ENTER_X_MIN
        val zLimit = if (isInPose) HOLD_Z_MAX else ENTER_Z_MAX
        val matches =
            magnitude in MIN_GRAVITY..MAX_GRAVITY &&
                abs(x) / magnitude >= xLimit &&
                abs(z) / magnitude <= zLimit

        if (matches == isInPose) {
            candidateSinceNs = null
        } else {
            val since = candidateSinceNs ?: timestampNs.also { candidateSinceNs = it }
            val dwell = if (matches) ENTER_DWELL_NS else EXIT_DWELL_NS
            if (timestampNs - since >= dwell) {
                isInPose = matches
                candidateSinceNs = null
            }
        }
        return isInPose
    }

    fun reset() {
        isInPose = false
        candidateSinceNs = null
        lastTimestampNs = null
    }

    private companion object {
        const val ENTER_X_MIN = 0.85f
        const val ENTER_Z_MAX = 0.40f
        const val HOLD_X_MIN = 0.65f
        const val HOLD_Z_MAX = 0.65f
        const val MIN_GRAVITY = 7f
        const val MAX_GRAVITY = 12f
        const val ENTER_DWELL_NS = 600_000_000L
        const val EXIT_DWELL_NS = 900_000_000L
        const val MAX_SAMPLE_GAP_NS = 250_000_000L
    }
}
