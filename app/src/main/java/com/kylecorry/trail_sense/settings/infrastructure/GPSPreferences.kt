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

    override val accuracyFilter by StringEnumPreference(
        cache,
        getString(R.string.pref_gps_accuracy_requirement),
        GPSAccuracyFilter.entries.associateBy { it.id.toString() },
        GPSAccuracyFilter.Low
    )

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
