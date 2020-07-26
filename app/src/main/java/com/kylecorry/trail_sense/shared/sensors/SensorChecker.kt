package com.kylecorry.trail_sense.shared.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.getSystemService

class SensorChecker(private val context: Context) {

    private val sensorManager = context.getSystemService<SensorManager>()

    fun hasBarometer(): Boolean {
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_PRESSURE) ?: return false
        return sensors.isNotEmpty()
    }

    fun hasGPS(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasGravity(): Boolean {
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_GRAVITY)
        return sensors?.isNotEmpty() ?: false
    }

}