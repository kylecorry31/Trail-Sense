package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.andromeda.preferences.Preferences

data class PreferenceMigration(
    val fromVersion: Int,
    val toVersion: Int,
    val action: (context: Context, prefs: Preferences) -> Unit
)