package com.kylecorry.trail_sense.tools.ruler.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class RulerPreferences(private val context: Context) {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    var rulerScale: Float
        get() {
            val raw = cache.getString(context.getString(R.string.pref_ruler_calibration)) ?: "1"
            return raw.toFloatCompat() ?: 1f
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_ruler_calibration), value.toString())
        }

    var rulerFlipped by BooleanPreference(
        cache,
        context.getString(R.string.pref_ruler_flip_direction),
        false
    )

}