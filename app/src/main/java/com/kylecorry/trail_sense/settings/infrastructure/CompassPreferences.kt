package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.trail_sense.R

class CompassPreferences(context: Context) : PreferenceRepo(context) {
    var useLegacyCompass: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_legacy_compass)) ?: false
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_use_legacy_compass),
            value
        )

    var compassSmoothing: Int
        get() = cache.getInt(context.getString(R.string.pref_compass_filter_amt)) ?: 1
        set(value) = cache.putInt(
            context.getString(R.string.pref_compass_filter_amt),
            value
        )

    var useTrueNorth: Boolean
        get() = (cache.getBoolean(
            context.getString(R.string.pref_use_true_north)
        ) ?: true
                ) && GPS.isAvailable(context)
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_use_true_north),
            value
        )
}