package com.kylecorry.trail_sense.weather

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.AbstractSensor

/**
 * A barometer sensor
 */
class Barometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL) {

    /**
     * The temperature in hPa
     */
    var pressure: Float = 0f
        private set

    override fun handleSensorEvent(event: SensorEvent) {
        pressure = event.values[0]
    }
}