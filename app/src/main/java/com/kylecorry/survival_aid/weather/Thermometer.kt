package com.kylecorry.survival_aid.weather

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.survival_aid.AbstractSensor

/**
 * A thermometer sensor
 */
class Thermometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_AMBIENT_TEMPERATURE, SensorManager.SENSOR_DELAY_NORMAL) {

    /**
     * The temperature in Celsius
     */
    var temperature: Float = 0f
        private set

    override fun handleSensorEvent(event: SensorEvent) {
        temperature = event.values[0]
    }
}