package com.kylecorry.trail_sense.tools.speedometer.infrastructure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trailsensecore.infrastructure.sensors.BaseSensor

class Pedometer(context: Context): BaseSensor(context, Sensor.TYPE_STEP_COUNTER, SensorManager.SENSOR_DELAY_NORMAL) {

    val steps: Int
        get() = _steps
    private var _steps = 0

    private var _hasReading = false

    override val hasValidReading: Boolean
        get() = _hasReading

    override fun handleSensorEvent(event: SensorEvent) {
        _steps = event.values[0].toInt()
        _hasReading = true
    }
}