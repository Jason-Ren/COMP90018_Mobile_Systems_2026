package com.comp90018.flashcards.ui.play

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ForeheadPoseDetectorTest {
    private val manager = mock<SensorManager>()
    private val context =
        mock<Context>().also {
            whenever(it.getSystemService(Context.SENSOR_SERVICE)).thenReturn(manager)
        }

    @Test
    fun `a phone with no motion sensor keeps manual play available`() {
        val detector = ForeheadPoseDetector(context) {}
        assertFalse(detector.start())
    }

    @Test
    fun `a registration failure does not advertise automatic pose support`() {
        val gravity = mock<Sensor>()
        whenever(manager.getDefaultSensor(Sensor.TYPE_GRAVITY)).thenReturn(gravity)
        val detector = ForeheadPoseDetector(context) {}
        assertFalse(detector.start())
    }

    @Test
    fun `starting twice registers once and stopping releases the listener`() {
        val gravity = mock<Sensor>()
        whenever(manager.getDefaultSensor(Sensor.TYPE_GRAVITY)).thenReturn(gravity)
        whenever(manager.registerListener(any(), eq(gravity), eq(SensorManager.SENSOR_DELAY_GAME))).thenReturn(true)
        val detector = ForeheadPoseDetector(context) {}

        assertTrue(detector.start())
        assertTrue(detector.start())
        verify(manager, times(1)).registerListener(detector, gravity, SensorManager.SENSOR_DELAY_GAME)
        detector.stop()
        // start resets a previous subscription; stop unregisters the active subscription.
        verify(manager, times(2)).unregisterListener(detector)
    }

    @Test
    fun `rotation vectors and accelerometer can each supply pose when gravity is absent`() {
        for (type in listOf(
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_ACCELEROMETER,
        )) {
            val fallbackManager = mock<SensorManager>()
            val fallbackContext = mock<Context>()
            val fallback = mock<Sensor>()
            whenever(fallbackContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(fallbackManager)
            whenever(fallbackManager.getDefaultSensor(type)).thenReturn(fallback)
            whenever(fallbackManager.registerListener(any(), eq(fallback), eq(SensorManager.SENSOR_DELAY_GAME)))
                .thenReturn(true)

            val detector = ForeheadPoseDetector(fallbackContext) {}
            assertTrue(detector.start())
            detector.stop()
            verify(fallbackManager, times(2)).unregisterListener(detector)
        }
    }
}
