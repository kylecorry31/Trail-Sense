package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.andromeda.preferences.BooleanPreference

class DepthPreferences(context: Context): PreferenceRepo(context) {

    var isDepthEnabled by BooleanPreference(cache, context.getString(R.string.pref_depth_enabled), false)

}