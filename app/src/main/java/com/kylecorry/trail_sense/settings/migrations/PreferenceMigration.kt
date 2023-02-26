package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.andromeda.preferences.IPreferences

data class PreferenceMigration(
    val fromVersion: Int,
    val toVersion: Int,
    val action: (context: Context, prefs: IPreferences) -> Unit
)