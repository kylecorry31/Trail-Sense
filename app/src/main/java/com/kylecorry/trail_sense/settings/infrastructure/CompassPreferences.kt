package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource

class CompassPreferences(context: Context) : PreferenceRepo(context), ICompassPreferences {

    private val sensors = SensorService(context)

    override var compassSmoothing by IntPreference(
        cache,
        context.getString(R.string.pref_compass_filter_amt),
        1
    )

    private var _useTrueNorthPref by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_true_north),
        true
    )

    override var useTrueNorth
        get() = !sensors.hasCompass() || _useTrueNorthPref
        set(value) {
            _useTrueNorthPref = value
        }

    override var source by StringEnumPreference(
        cache,
        context.getString(R.string.pref_compass_source),
        CompassSource.entries.associateBy { it.id },
        CompassSource.RotationVector
    )
}