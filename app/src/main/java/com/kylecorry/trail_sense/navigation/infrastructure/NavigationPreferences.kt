package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import kotlin.math.min

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

    val useLegacyCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_legacy_compass), false)

    val compassSmoothing: Int
        get() = prefs.getInt(context.getString(R.string.pref_compass_filter_amt), 1)

    val showLinearCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_show_linear_compass), true)

    val showMultipleBeacons: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_display_multi_beacons), false)

    val numberOfVisibleBeacons: Int
        get(){
            val raw = prefs.getString(context.getString(R.string.pref_num_visible_beacons), "1") ?: "1"
            return raw.toInt()
        }

    val rulerScale: Float
        get(){
            val raw = prefs.getString(context.getString(R.string.pref_ruler_calibration), "1") ?: "1"
            return raw.toFloat()
        }

    val averageSpeed: Float
        get() = prefs.getFloat(context.getString(R.string.pref_average_speed), 0f)

    fun setAverageSpeed(metersPerSecond: Float){
        prefs.edit {
            putFloat(context.getString(R.string.pref_average_speed), min(metersPerSecond, 3f))
        }
    }

    enum class AltimeterMode {
        Barometer,
        GPS,
        None
    }

}