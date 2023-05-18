package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R

class CompassPreferences(context: Context) : PreferenceRepo(context) {

    var useLegacyCompass by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_legacy_compass),
        false
    )

    var compassSmoothing by IntPreference(
        cache,
        context.getString(R.string.pref_compass_filter_amt),
        1
    )

    var useTrueNorth by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_true_north),
        true
    )
}