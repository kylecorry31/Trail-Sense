package com.kylecorry.trail_sense.shared.sensors

import android.app.Service
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class SensorChecker(context: Context) {

    private val sensorManager = context.getSystemService(Service.SENSOR_SERVICE) as SensorManager

    fun hasBarometer(): Boolean {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_PRESSURE)
        return sensors.isNotEmpty()
    }

}