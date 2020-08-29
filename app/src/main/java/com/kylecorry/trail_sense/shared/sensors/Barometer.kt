package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

class Barometer(context: Context): BaseSensor(context, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL), IBarometer {

    override val pressure: Float
        get() = _pressure

    override val hasValidReading: Boolean
        get() = gotReading

    override val altitude: Float
        get() = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)

    private var _pressure = 0f
    private var gotReading = false

    override fun handleSensorEvent(event: SensorEvent) {
        gotReading = true
        _pressure = event.values[0]
    }

}