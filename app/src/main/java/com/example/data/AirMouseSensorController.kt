package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Converts Android gyroscope angular velocity into smooth Windows cursor deltas for Air Mouse mode.
 * Applies a small deadzone to filter hand tremor and sensor drift.
 */
class AirMouseSensorController(
    context: Context,
    private val onCursorDelta: (Float, Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAvailable: Boolean
        get() = gyroSensor != null

    @Volatile
    private var active: Boolean = false

    @Volatile
    private var sensitivity: Float = 1.25f

    private var lastTimestampNs: Long = 0L

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(0.4f, 3.0f)
    }

    fun start(): Boolean {
        val manager = sensorManager ?: return false
        val sensor = gyroSensor ?: return false
        if (active) return true
        lastTimestampNs = 0L
        active = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        return active
    }

    fun stop() {
        if (!active) return
        active = false
        lastTimestampNs = 0L
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!active || event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val nowNs = event.timestamp
        if (lastTimestampNs == 0L) {
            lastTimestampNs = nowNs
            return
        }
        val dtSec = ((nowNs - lastTimestampNs) / 1_000_000_000f).coerceIn(0.001f, 0.08f)
        lastTimestampNs = nowNs

        val (dx, dy) = mapGyroToCursorDelta(
            pitchRadPerSec = event.values[0],
            yawRadPerSec = event.values[2],
            rollRadPerSec = event.values[1],
            dtSec = dtSec,
            sensitivity = sensitivity
        )
        if (dx != 0f || dy != 0f) {
            onCursorDelta(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val DEADZONE_RAD_S = 0.045f
        private const val BASE_GAIN = 260f

        fun mapGyroToCursorDelta(
            pitchRadPerSec: Float,
            yawRadPerSec: Float,
            rollRadPerSec: Float,
            dtSec: Float,
            sensitivity: Float
        ): Pair<Float, Float> {
            // Combine natural wrist yaw (Z) and slight side tilt (Y) for comfortable wand aiming
            val horizontalRate = -(yawRadPerSec + rollRadPerSec * 0.35f)
            val verticalRate = -pitchRadPerSec

            val filteredX = if (abs(horizontalRate) < DEADZONE_RAD_S) 0f else horizontalRate
            val filteredY = if (abs(verticalRate) < DEADZONE_RAD_S) 0f else verticalRate

            val scale = BASE_GAIN * sensitivity * dtSec
            return (filteredX * scale) to (filteredY * scale)
        }
    }
}
