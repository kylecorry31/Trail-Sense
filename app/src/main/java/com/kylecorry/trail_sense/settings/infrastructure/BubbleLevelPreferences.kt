package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class BubbleLevelPreferences(private val context: Context) : IBubbleLevelPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    override var threshold by FloatPreference(
        cache,
        context.getString(R.string.pref_bubble_level_threshold),
        2.0f
    )
}
