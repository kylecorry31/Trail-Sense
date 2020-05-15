package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R

class NavigationPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val altimeter: AltimeterMode
        get(){
            val modePref = prefs.getString(context.getString(R.string.pref_altitude_mode), "gps")
            return if (modePref == "gps"){
                AltimeterMode.GPS
            } else {
                AltimeterMode.Barometer
            }
        }

    val useTrueNorth: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_true_north), true)

    val useExperimentalCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_experimental_compass), false)

    val distanceUnits: String
        get() = prefs.getString(context.getString(R.string.pref_distance_units), "meters") ?: "meters"

    enum class AltimeterMode {
        Barometer,
        GPS
    }

}