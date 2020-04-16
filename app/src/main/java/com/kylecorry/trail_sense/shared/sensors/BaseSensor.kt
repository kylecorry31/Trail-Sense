package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

abstract class BaseSensor(context: Context, private val sensorType: Int, private val sensorDelay: Int): AbstractSensor() {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            handleSensorEvent(event)
            notifyListeners()
        }

    }

    override fun startImpl() {
        sensorManager.getDefaultSensor(sensorType)?.also { sensor ->
            sensorManager.registerListener(
                sensorListener,
                sensor,
                sensorDelay
            )
        }
    }

    override fun stopImpl() {
        sensorManager.unregisterListener(sensorListener)
    }

    protected abstract fun handleSensorEvent(event: SensorEvent)
}