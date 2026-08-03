package com.kylecorry.trail_sense.tools.tides.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tides.infrastructure.ITidePreferences
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class TidePreferences(context: Context) : PreferenceRepo(context), ITidePreferences {

    override val showNearestTide by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_nearest_tide),
        false
    )

    override var lastTide: Long?
        get() = cache.getLong(context.getString(R.string.last_tide_id))
        set(value) {
            if (value != null) {
                cache.putLong(context.getString(R.string.last_tide_id), value)
            } else {
                cache.remove(context.getString(R.string.last_tide_id))
            }
        }
}
