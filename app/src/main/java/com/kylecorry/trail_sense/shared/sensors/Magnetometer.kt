package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.weather.domain.MovingAverageFilter

class Magnetometer(context: Context): BaseSensor(context, Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_FASTEST) {

    private val filterSize = 20

    private val filters = listOf(
        MovingAverageFilter(filterSize),
        MovingAverageFilter(filterSize),
        MovingAverageFilter(filterSize)
    )

   var magneticField: FloatArray = FloatArray(3)
    private set

    override fun handleSensorEvent(event: SensorEvent) {
        magneticField = event.values.mapIndexed { index, value -> filters[index].filter(value.toDouble()).toFloat() }.toFloatArray()
    }

}