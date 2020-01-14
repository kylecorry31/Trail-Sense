package com.kylecorry.trail_sense.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.AbstractSensor
import com.kylecorry.trail_sense.sensors.IBarometer

/**
 * A barometer sensor
 */
class Barometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL),
    IBarometer {

    private var _pressure: Float = 0F

    override val pressure: Float
        get() = _pressure

    override fun handleSensorEvent(event: SensorEvent) {
        _pressure = event.values[0]
    }
}