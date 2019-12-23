package com.kylecorry.survival_aid.weather

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.survival_aid.AbstractSensor

/**
 * A humidity sensor / hygrometer
 */
class Hygrometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_RELATIVE_HUMIDITY, SensorManager.SENSOR_DELAY_NORMAL) {

    /**
     * The humidity level
     */
    var humidity: Float = 0f
        private set

    override fun handleSensorEvent(event: SensorEvent) {
        humidity = event.values[0]
    }
}