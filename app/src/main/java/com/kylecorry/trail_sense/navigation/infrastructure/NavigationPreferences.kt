package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.locationformat.ILocationFormatter
import com.kylecorry.trail_sense.navigation.domain.locationformat.LocationDecimalDegreesFormatter
import com.kylecorry.trail_sense.navigation.domain.locationformat.LocationDegreesDecimalMinuteFormatter
import com.kylecorry.trail_sense.navigation.domain.locationformat.LocationDegreesMinuteSecondFormatter
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import kotlin.math.min

class NavigationPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensorChecker = SensorChecker(context)

    var useTrueNorth: Boolean
        get() = prefs.getBoolean(
            context.getString(R.string.pref_use_true_north),
            true
        ) && sensorChecker.hasGPS()
        set(value) = prefs.edit {
            putBoolean(
                context.getString(R.string.pref_use_true_north),
                value
            )
        }

    val showCalibrationOnNavigateDialog: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_show_calibrate_on_navigate_dialog), true)

    var useLegacyCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_legacy_compass), false)
        set(value) = prefs.edit {
            putBoolean(
                context.getString(R.string.pref_use_legacy_compass),
                value
            )
        }

    var compassSmoothing: Int
        get() = prefs.getInt(context.getString(R.string.pref_compass_filter_amt), 1)
        set(value) = prefs.edit { putInt(context.getString(R.string.pref_compass_filter_amt), value) }

    val showLinearCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_show_linear_compass), true)

    val showMultipleBeacons: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_display_multi_beacons), false)

    val numberOfVisibleBeacons: Int
        get() {
            val raw =
                prefs.getString(context.getString(R.string.pref_num_visible_beacons), "1") ?: "1"
            return raw.toInt()
        }

    val rulerScale: Float
        get() {
            val raw =
                prefs.getString(context.getString(R.string.pref_ruler_calibration), "1") ?: "1"
            return raw.toFloat()
        }

    val averageSpeed: Float
        get() = prefs.getFloat(context.getString(R.string.pref_average_speed), 0f)

    fun setAverageSpeed(metersPerSecond: Float) {
        prefs.edit {
            putFloat(context.getString(R.string.pref_average_speed), min(metersPerSecond, 3f))
        }
    }

    var showBeaconListToast: Boolean
        get() = prefs.getBoolean("show_beacon_list_toast", true)
        set(value) {
            prefs.edit {
                putBoolean("show_beacon_list_toast", value)
            }
        }

    fun formatLocation(location: Coordinate): String {
        val formatter = locationFormatter
        val lat = formatter.formatLatitude(location)
        val lng = formatter.formatLongitude(location)
        return getFormattedLocation(lat, lng)
    }

    private fun getFormattedLocation(latitude: String, longitude: String): String {
        return context.getString(locationFormat, latitude, longitude)
    }

    private val locationFormat: Int
        get() {
            return when (prefs.getString(
                context.getString(R.string.pref_coordinate_format),
                "dms"
            )) {
                "dd" -> R.string.coordinate_format_string_dd
                "ddm" -> R.string.coordinate_format_string_ddm
                else -> R.string.coordinate_format_string_dms
            }
        }

    private val locationFormatter: ILocationFormatter
        get() {
            return when (prefs.getString(
                context.getString(R.string.pref_coordinate_format),
                "dms"
            )) {
                "dd" -> LocationDecimalDegreesFormatter()
                "ddm" -> LocationDegreesDecimalMinuteFormatter()
                else -> LocationDegreesMinuteSecondFormatter()
            }
        }

}