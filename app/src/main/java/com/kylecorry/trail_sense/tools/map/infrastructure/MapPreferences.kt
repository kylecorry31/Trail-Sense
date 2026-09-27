package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.sensors.compass.CompassUpdateFrequency

class MapPreferences(context: Context) : PreferenceRepo(context) {

    val compassUpdateFrequency by StringEnumPreference(
        cache,
        context.getString(R.string.pref_map_compass_update_frequency),
        CompassUpdateFrequency.entries.associateBy { it.id },
        CompassUpdateFrequency.Normal
    )

    val keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        context.getString(R.string.pref_map_keep_unlocked),
        false
    )

    val saveMapState by BooleanPreference(
        cache,
        context.getString(R.string.pref_save_map_state),
        false
    )

    val highDetailMode by BooleanPreference(
        cache,
        context.getString(R.string.pref_map_high_detail_mode),
        false
    )
}
