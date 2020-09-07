package com.kylecorry.trail_sense.shared.sensors.temperature

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.shared.sensors.BaseSensor

class Thermometer(context: Context, sensorCode: Int = Sensor.TYPE_AMBIENT_TEMPERATURE) :
    BaseSensor(context, sensorCode, SensorManager.SENSOR_DELAY_FASTEST),
    IThermometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = false

    private var _temp = 0f

    override val temperature: Float
        get() = _temp

    override fun handleSensorEvent(event: SensorEvent) {
        _temp = event.values[0]
        gotReading = true
    }

}