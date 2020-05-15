package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.weather.domain.LowPassFilter

class Accelerometer(context: Context): BaseSensor(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST) {

    val filterSize = 0.03
    val filters = listOf(
        LowPassFilter(filterSize),
        LowPassFilter(filterSize),
        LowPassFilter(filterSize)
    )

    var acceleration = FloatArray(3)
        private set

    override fun handleSensorEvent(event: SensorEvent) {
        acceleration = event.values.mapIndexed { index, value -> filters[index].filter(value.toDouble()).toFloat() }.toFloatArray()
    }

}