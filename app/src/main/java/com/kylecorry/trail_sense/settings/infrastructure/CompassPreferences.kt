package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R

class CompassPreferences(context: Context) : PreferenceRepo(context) {

    val useHighAccuracy by BooleanPreference(
        cache,
        getString(R.string.pref_high_accuracy_compass),
        false
    )

}