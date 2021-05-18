package com.kylecorry.trail_sense.settings

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker

class MetalDetectorPreferences(context: Context): PreferenceRepo(context) {

    private val sensorChecker by lazy { SensorChecker(context) }

    val showMetalDirection: Boolean
        get(){
            val hasGyro = sensorChecker.hasSensor(Sensor.TYPE_GYROSCOPE)
            val experimental = cache.getBoolean(getString(R.string.pref_enable_experimental)) ?: false
            return hasGyro && experimental
        }

    val showSinglePole: Boolean = true

    val directionSensitivity: Float = 0.6f

}