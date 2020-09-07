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
        return hasSensor(Sensor.TYPE_PRESSURE)
    }

    fun hasGPS(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasGravity(): Boolean {
        return hasSensor(Sensor.TYPE_GRAVITY)
    }

    fun hasThermometer(): Boolean {
        return true
    }

    fun hasHygrometer(): Boolean {
        return hasSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
    }

    fun hasSensor(sensorCode: Int): Boolean {
        val sensors = sensorManager?.getSensorList(sensorCode)
        return sensors?.isNotEmpty() ?: false
    }

    fun hasSensorLike(name: String): Boolean {
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_ALL)
        return sensors?.any { it.name.contains(name, true) } ?: false
    }

}