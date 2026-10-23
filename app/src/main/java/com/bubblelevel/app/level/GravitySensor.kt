package com.bubblelevel.app.level

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Smoothed gravity vector; the fused gravity sensor when present, else the raw accelerometer. */
class GravitySensor(context: Context) : SensorEventListener {

    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val available = sensor != null

    /** (x, y, z) in m/s², null until the first reading. */
    var gravity by mutableStateOf<FloatArray?>(null)
        private set

    fun start() {
        sensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val prev = gravity
        gravity = if (prev == null) {
            event.values.copyOf(3)
        } else {
            // Low-pass: steadies the 0.1° readout and filters hand shake out of the accelerometer.
            FloatArray(3) { prev[it] + SMOOTHING * (event.values[it] - prev[it]) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SMOOTHING = 0.1f
    }
}
