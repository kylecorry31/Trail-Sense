package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource

class CompassPreferences(context: Context) : PreferenceRepo(context), ICompassPreferences {

    override var compassSmoothing by IntPreference(
        cache,
        context.getString(R.string.pref_compass_filter_amt),
        1
    )

    override var useTrueNorth by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_true_north),
        true
    )

    override var source by StringEnumPreference(
        cache,
        context.getString(R.string.pref_compass_source),
        CompassSource.values().associateBy { it.id },
        CompassSource.RotationVector
    )
}