package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import kotlin.math.min

class NavigationPreferences(private val context: Context) {

    private val cache by lazy { Cache(context) }
    private val sensorChecker by lazy { SensorChecker(context) }
    private val unitService = UnitService()

    var useTrueNorth: Boolean
        get() = (cache.getBoolean(
            context.getString(R.string.pref_use_true_north)
        ) ?: true
                ) && sensorChecker.hasGPS()
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_use_true_north),
            value
        )

    val showCalibrationOnNavigateDialog: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_calibrate_on_navigate_dialog)
        ) ?: true

    var useLegacyCompass: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_legacy_compass)) ?: false
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_use_legacy_compass),
            value
        )

    var compassSmoothing: Int
        get() = cache.getInt(context.getString(R.string.pref_compass_filter_amt)) ?: 1
        set(value) = cache.putInt(
            context.getString(R.string.pref_compass_filter_amt),
            value
        )

    val showLinearCompass: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_linear_compass)) ?: true

    val showMultipleBeacons: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_display_multi_beacons)) ?: false

    val numberOfVisibleBeacons: Int
        get() {
            val raw = cache.getString(context.getString(R.string.pref_num_visible_beacons)) ?: "1"
            return raw.toIntOrNull() ?: 1
        }

    var maxBeaconDistance: Float
        get() {
            val raw = cache.getString(context.getString(R.string.pref_max_beacon_distance)) ?: "100"
            return unitService.convert(
                raw.toFloatOrNull() ?: 100f,
                DistanceUnits.Kilometers,
                DistanceUnits.Meters
            )
        }
        set(value) = cache.putString(
            context.getString(R.string.pref_max_beacon_distance),
            unitService.convert(value, DistanceUnits.Meters, DistanceUnits.Kilometers)
                .toString()
        )

    val rulerScale: Float
        get() {
            val raw = cache.getString(context.getString(R.string.pref_ruler_calibration)) ?: "1"
            return raw.toFloatOrNull() ?: 1f
        }

    val averageSpeed: Float
        get() = cache.getFloat(context.getString(R.string.pref_average_speed)) ?: 0f

    fun setAverageSpeed(metersPerSecond: Float) {
        cache.putFloat(context.getString(R.string.pref_average_speed), min(metersPerSecond, 3f))
    }

    val coordinateFormat: CoordinateFormat
        get() {
            return when (cache.getString(context.getString(R.string.pref_coordinate_format))) {
                "dms" -> CoordinateFormat.DegreesMinutesSeconds
                "ddm" -> CoordinateFormat.DegreesDecimalMinutes
                "utm" -> CoordinateFormat.UTM
                "mgrs" -> CoordinateFormat.MGRS
                else -> CoordinateFormat.DecimalDegrees
            }
        }


    val factorInNonLinearDistance: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_non_linear_distances)) ?: true

}