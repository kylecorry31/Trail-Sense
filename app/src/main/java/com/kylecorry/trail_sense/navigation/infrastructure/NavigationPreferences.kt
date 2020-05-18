package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorChecker

class NavigationPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensorChecker = SensorChecker(context)

    val altimeter: AltimeterMode
        get(){
            val hasBarometer = sensorChecker.hasBarometer()
            val hasGPS = sensorChecker.hasGPS()

            val modePref = prefs.getString(context.getString(R.string.pref_altitude_mode), "gps")
            return if (modePref == "gps" && hasGPS){
                AltimeterMode.GPS
            } else if (hasBarometer) {
                AltimeterMode.Barometer
            } else {
                AltimeterMode.None
            }
        }

    val useTrueNorth: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_true_north), true) && sensorChecker.hasGPS()

    val useExperimentalCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_experimental_compass), false)

    val compassSmoothing: Int
        get() = prefs.getInt(context.getString(R.string.pref_compass_filter_amt), 1)

    val showRuler: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_show_ruler), true)

    enum class AltimeterMode {
        Barometer,
        GPS,
        None
    }

}