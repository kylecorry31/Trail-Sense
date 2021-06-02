package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.BooleanPreference

class PowerPreferences(context: Context): PreferenceRepo(context) {

    val areTilesEnabled by BooleanPreference(cache, getString(R.string.pref_tiles_enabled), true)

}