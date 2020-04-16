package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing

class Compass(context: Context): BaseSensor(context, Sensor.TYPE_ORIENTATION, SensorManager.SENSOR_DELAY_FASTEST), ICompass {

    override var declination = 0f

    override val bearing: Bearing
        get() = Bearing(_bearing + declination)

    private var _bearing = 0f

    override fun handleSensorEvent(event: SensorEvent) {
        _bearing = event.values[0]
    }

}