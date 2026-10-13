package com.comp90018.flashcards.domain.sensors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForeheadPoseClassifierTest {
    private val detector = ForeheadPoseClassifier()
    private var timeNs = 0L

    private fun hold(
        x: Float,
        y: Float,
        z: Float,
        milliseconds: Int,
    ) {
        repeat(milliseconds / 100 + 1) {
            detector.update(x, y, z, timeNs)
            timeNs += 100_000_000L
        }
    }

    @Test
    fun `both landscape directions enter after a stable hold`() {
        for (sign in listOf(-1, 1)) {
            detector.reset()
            hold(sign * 9.81f, 0f, 0f, 500)
            assertFalse(detector.isInPose)
            hold(sign * 9.81f, 0f, 0f, 100)
            assertTrue(detector.isInPose)
        }
    }

    @Test
    fun `portrait flat and invalid vectors never start a round`() {
        for (vector in listOf(
            floatArrayOf(0f, 9.81f, 0f),
            floatArrayOf(0f, 0f, 9.81f),
            floatArrayOf(0f, 0f, -9.81f),
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(20f, 0f, 0f),
            floatArrayOf(Float.NaN, 0f, 0f),
            floatArrayOf(Float.POSITIVE_INFINITY, 0f, 0f),
        )) {
            hold(vector[0], vector[1], vector[2], 1_000)
            assertFalse(detector.isInPose)
        }
    }

    @Test
    fun `a brief scoring tilt does not leave the pose`() {
        hold(9.81f, 0f, 0f, 600)
        hold(0f, 0f, 9.81f, 700)
        assertTrue(detector.isInPose)
        hold(9.81f, 0f, 0f, 100)
        assertTrue(detector.isInPose)
        hold(0f, 9.81f, 0f, 900)
        assertFalse(detector.isInPose)
    }

    @Test
    fun `hold thresholds prevent boundary jitter from repeatedly entering and leaving`() {
        hold(9.81f, 0f, 0f, 600)
        // Roughly 40 degrees off upright, outside entry but inside the wider hold thresholds.
        hold(7.5f, 0f, 6.3f, 2_000)
        assertTrue(detector.isInPose)
        detector.reset()
        hold(7.5f, 0f, 6.3f, 2_000)
        assertFalse(detector.isInPose)
    }

    @Test
    fun `interrupted entry and sparse samples do not count as a continuous hold`() {
        hold(9.81f, 0f, 0f, 400)
        hold(0f, 9.81f, 0f, 100)
        hold(9.81f, 0f, 0f, 400)
        assertFalse(detector.isInPose)
        timeNs += 2_000_000_000L
        hold(9.81f, 0f, 0f, 100)
        assertFalse(detector.isInPose)
    }

    @Test
    fun `reset requires fresh dwell and out of order samples are ignored`() {
        hold(9.81f, 0f, 0f, 600)
        detector.update(0f, 9.81f, 0f, 0L)
        assertTrue(detector.isInPose)
        detector.reset()
        assertFalse(detector.isInPose)
        hold(9.81f, 0f, 0f, 100)
        assertFalse(detector.isInPose)
    }
}
