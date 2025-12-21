package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.R

class BubbleLevelPreferences(context: Context) : PreferenceRepo(context) {

    var threshold by FloatPreference(
        cache,
        context.getString(R.string.pref_bubble_level_threshold),
        2.0f
    )
}
