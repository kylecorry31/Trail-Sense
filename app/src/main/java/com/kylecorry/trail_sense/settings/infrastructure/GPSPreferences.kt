package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode

class GPSPreferences(context: Context) : PreferenceRepo(context), IGPSPreferences {

    override var locationSource by StringEnumPreference(
        cache,
        getString(R.string.pref_auto_location),
        GPSLocationSource.entries.associateBy { it.id },
        GPSLocationSource.GPS
    )

    override var accuracyFilter: GPSAccuracyFilter
        get() {
            val accuracy = cache.getFloat(getString(R.string.pref_gps_accuracy_meters))
                ?: GPSAccuracyFilter.Default.minAccuracy!!
            val wait = cache.getInt(getString(R.string.pref_gps_accuracy_wait_seconds))
                ?: GPSAccuracyFilter.Default.maxAccuracyWait!!.seconds.toInt()
            if (accuracy <= 0 || wait <= 0) {
                return GPSAccuracyFilter.None
            }
            return GPSAccuracyFilter.custom(accuracy, wait)
        }
        set(value) {
            if (value.minAccuracy == null || value.maxAccuracyWait == null) {
                cache.putFloat(getString(R.string.pref_gps_accuracy_meters), 0f)
                cache.putInt(getString(R.string.pref_gps_accuracy_wait_seconds), 0)
                return
            }
            val accuracy = value.minAccuracy
            val wait = value.maxAccuracyWait.seconds.toInt()
            cache.putFloat(
                getString(R.string.pref_gps_accuracy_meters),
                accuracy.coerceIn(
                    GPSAccuracyFilter.MIN_ACCURACY_METERS.toFloat(),
                    GPSAccuracyFilter.MAX_ACCURACY_METERS.toFloat()
                )
            )
            cache.putInt(
                getString(R.string.pref_gps_accuracy_wait_seconds),
                wait.coerceIn(
                    GPSAccuracyFilter.MIN_WAIT_SECONDS,
                    GPSAccuracyFilter.MAX_WAIT_SECONDS
                )
            )
        }

    override val smoothing: Int
        get() = (cache.getInt(getString(R.string.pref_gps_smoothing)) ?: 0).coerceIn(0, 100)

    override val powerMode by StringEnumPreference(
        cache,
        getString(R.string.pref_gps_power_usage),
        GPSPowerMode.entries.associateBy { it.id.toString() },
        GPSPowerMode.High
    )

    override var locationOverride: Coordinate
        get() {
            val latStr = cache.getString(getString(R.string.pref_latitude_override)) ?: "0.0"
            val lngStr = cache.getString(getString(R.string.pref_longitude_override)) ?: "0.0"

            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            return Coordinate(lat, lng)
        }
        set(value) {
            cache.putString(getString(R.string.pref_latitude_override), value.latitude.toString())
            cache.putString(getString(R.string.pref_longitude_override), value.longitude.toString())
        }

    override val hasLocationOverride: Boolean
        get() = cache.contains(getString(R.string.pref_latitude_override)) &&
                cache.contains(getString(R.string.pref_longitude_override))
}
