package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.shared.domain.Vector3
import com.kylecorry.trail_sense.weather.domain.LowPassFilter

class GravitySensor(context: Context) :
    BaseSensor(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST), IAccelerometer {

    private val filterSize = 0.03f
    private val filters = listOf(
        LowPassFilter(filterSize),
        LowPassFilter(filterSize),
        LowPassFilter(filterSize)
    )

    private var _acceleration = Vector3.zero

    override val acceleration
        get() = _acceleration

    override fun handleSensorEvent(event: SensorEvent) {
        _acceleration = Vector3(
            filters[0].filter(event.values[0]),
            filters[1].filter(event.values[1]),
            filters[2].filter(event.values[2])
        )
    }

}