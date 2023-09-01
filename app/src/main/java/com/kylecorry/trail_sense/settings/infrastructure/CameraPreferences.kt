package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class CameraPreferences(private val context: Context) : ICameraPreferences {
    private val prefs by lazy { PreferencesSubsystem.getInstance(context).preferences }

    override val useZeroShutterLag by BooleanPreference(
        prefs,
        context.getString(R.string.pref_use_zero_shutter_lag),
        false
    )


}