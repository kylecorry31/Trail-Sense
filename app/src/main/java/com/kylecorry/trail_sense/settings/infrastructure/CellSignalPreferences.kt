package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences

class CellSignalPreferences(private val context: Context) {
    private val cache by lazy { Preferences(context) }

    val populateCache by BooleanPreference(
        cache,
        context.getString(R.string.pref_cell_signal_refresh_cache),
        true
    )

}