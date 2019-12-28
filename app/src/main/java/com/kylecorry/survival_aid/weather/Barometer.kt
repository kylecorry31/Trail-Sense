package com.kylecorry.survival_aid.weather

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.survival_aid.AbstractSensor

/**
 * A barometer sensor
 */
class Barometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL) {

    /**
     * The temperature in hPa
     */
    var pressure: Float = 0f
        private set

    val altitude: Float
        get() { return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure) }

    override fun handleSensorEvent(event: SensorEvent) {
        pressure = event.values[0]
    }
}