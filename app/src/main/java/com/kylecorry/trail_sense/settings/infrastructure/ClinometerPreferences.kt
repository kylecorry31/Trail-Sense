package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R

class ClinometerPreferences(private val context: Context) : IClinometerPreferences {

    private val cache by lazy { Preferences(context) }

    override val lockWithVolumeButtons by BooleanPreference(
        cache,
        context.getString(R.string.pref_clinometer_lock_with_volume_buttons),
        false
    )
}