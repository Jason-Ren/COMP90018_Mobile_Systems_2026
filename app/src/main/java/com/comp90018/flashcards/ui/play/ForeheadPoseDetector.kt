package com.comp90018.flashcards.ui.play

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.comp90018.flashcards.domain.sensors.ForeheadPoseClassifier
import kotlin.math.exp

/** Android adapter for fused gravity, with rotation-vector and filtered-accelerometer fallbacks. */
class ForeheadPoseDetector(
    context: Context,
    private val onPose: (Boolean) -> Unit,
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor =
        manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val classifier = ForeheadPoseClassifier()
    private val gravity = FloatArray(3)
    private val rotation = FloatArray(9)
    private var lastTimestampNs = 0L
    private var listening = false

    /** True when a usable sensor exists; known immediately, before registration. */
    val hasSensor: Boolean get() = sensor != null

    /** False when no sensor exists or registration fails; the screen still offers manual Start. */
    fun start(): Boolean {
        if (listening) return true
        stop()
        listening = sensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } == true
        return listening
    }

    fun stop() {
        listening = false
        manager.unregisterListener(this)
        classifier.reset()
        lastTimestampNs = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!listening || event.values.size < 3 || event.timestamp <= lastTimestampNs) return
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                // R maps device to world; its last row is world-up in device coordinates.
                for (axis in gravity.indices) gravity[axis] = rotation[6 + axis] * SensorManager.GRAVITY_EARTH
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val dt = (event.timestamp - lastTimestampNs) * 1e-9
                val alpha = if (lastTimestampNs == 0L) 0f else exp(-dt / FILTER_SECONDS).toFloat()
                for (axis in gravity.indices) {
                    gravity[axis] = alpha * gravity[axis] + (1f - alpha) * event.values[axis]
                }
            }
            else -> event.values.copyInto(gravity, endIndex = 3)
        }
        lastTimestampNs = event.timestamp
        // Publish the stable state on each sample so a newly loaded/replayed deck can use a held pose.
        onPose(classifier.update(gravity[0], gravity[1], gravity[2], event.timestamp))
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    private companion object {
        const val FILTER_SECONDS = 0.2
    }
}
