package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

class Accelerometer(context: Context): BaseSensor(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST) {

   var acceleration: FloatArray = FloatArray(3)
    private set

    override fun handleSensorEvent(event: SensorEvent) {
        acceleration = event.values.clone()
    }

}