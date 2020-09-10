package com.kylecorry.trail_sense.shared.sensors.hygrometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.shared.sensors.BaseSensor

class Hygrometer(context: Context) :
    BaseSensor(context, Sensor.TYPE_RELATIVE_HUMIDITY, SensorManager.SENSOR_DELAY_FASTEST),
    IHygrometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = false

    private var _humidity = 0f

    override val humidity: Float
        get() = _humidity

    override fun handleSensorEvent(event: SensorEvent) {
        _humidity = event.values[0]
        gotReading = true
    }

}