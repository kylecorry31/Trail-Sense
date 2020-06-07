package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.weather.domain.LowPassFilter

class LowPassAccelerometer(context: Context) :
    BaseSensor(context, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST), IAccelerometer {

    private val filterSize = 0.05
    private val filters = listOf(
        LowPassFilter(filterSize),
        LowPassFilter(filterSize),
        LowPassFilter(filterSize)
    )

    override val acceleration = FloatArray(3)

    override fun handleSensorEvent(event: SensorEvent) {
        event.values.forEachIndexed { index, value ->
            acceleration[index] = filters[index].filter(value.toDouble()).toFloat()
        }
    }

}