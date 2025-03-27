package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R

class MetalDetectorPreferences(context: Context): PreferenceRepo(context) {

    val showMetalDirection: Boolean
        get(){
            val hasGyro = Sensors.hasSensor(context, Sensor.TYPE_GYROSCOPE)
            val enabled = cache.getBoolean(getString(R.string.pref_experimental_metal_direction)) ?: false
            return hasGyro && enabled
        }

    val showSinglePole: Boolean = false

    val directionSensitivity: Float = 0.6f

    var isMetalAudioEnabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_metal_audio),
        false
    )
    var isMetalVibrationDisabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_no_use_metal_vibration),
        false
    )
}