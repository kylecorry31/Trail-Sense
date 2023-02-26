package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class ClinometerPreferences(private val context: Context) : IClinometerPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    override var lockWithVolumeButtons by BooleanPreference(
        cache,
        context.getString(R.string.pref_clinometer_lock_with_volume_buttons),
        false
    )
    override var baselineDistance: Distance?
        get() {
            val baseline =
                cache.getFloat(context.getString(R.string.pref_clinometer_baseline_distance))
                    ?: return null
            val baselineUnitId =
                cache.getInt(context.getString(R.string.pref_clinometer_baseline_distance_units))
                    ?: return null
            val units = DistanceUnits.values().firstOrNull { baselineUnitId == it.id }
                ?: DistanceUnits.Meters
            return Distance(baseline, units)
        }
        set(value) {
            if (value == null) {
                cache.remove(context.getString(R.string.pref_clinometer_baseline_distance))
                cache.remove(context.getString(R.string.pref_clinometer_baseline_distance_units))
            } else {
                cache.putFloat(
                    context.getString(R.string.pref_clinometer_baseline_distance),
                    value.distance
                )
                cache.putInt(
                    context.getString(R.string.pref_clinometer_baseline_distance_units),
                    value.units.id
                )
            }
        }

    override var measureHeightInstructionsSent by BooleanPreference(
        cache,
        "pref_clinometer_measure_height_read",
        false
    )

    override var measureDistanceInstructionsSent by BooleanPreference(
        cache,
        "pref_clinometer_measure_distance_read",
        false
    )
}