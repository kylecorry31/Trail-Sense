package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker

class MetalDetectorPreferences(context: Context): PreferenceRepo(context) {

    private val sensorChecker by lazy { SensorChecker(context) }

    val showMetalDirection: Boolean
        get(){
            val hasGyro = sensorChecker.hasSensor(Sensor.TYPE_GYROSCOPE)
            val enabled = cache.getBoolean(getString(R.string.pref_experimental_metal_direction)) ?: false
            return hasGyro && enabled
        }

    val showSinglePole: Boolean = false

    val directionSensitivity: Float = 0.6f

}