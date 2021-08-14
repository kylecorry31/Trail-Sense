package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.andromeda.preferences.BooleanPreference

class TidePreferences(context: Context): PreferenceRepo(context) {

    var areTidesEnabled by BooleanPreference(cache, context.getString(R.string.pref_experimental_tide_clock), false)

}