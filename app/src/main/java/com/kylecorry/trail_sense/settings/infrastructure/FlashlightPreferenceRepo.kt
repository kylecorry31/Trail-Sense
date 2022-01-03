package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import java.time.Duration

class FlashlightPreferenceRepo(context: Context) : PreferenceRepo(context) {

    var toggleWithSystem by BooleanPreference(
        cache,
        getString(R.string.pref_flashlight_toggle_with_system),
        true
    )

    var toggleWithVolumeButtons by BooleanPreference(
        cache,
        getString(R.string.pref_flashlight_toggle_with_volume),
        false
    )

    val shouldTimeout by BooleanPreference(
        cache,
        context.getString(R.string.pref_flashlight_should_timeout),
        false
    )

    var timeout: Duration
        get() {
            val seconds =
                cache.getLong(context.getString(R.string.pref_flashlight_timeout)) ?: (5L * 60)
            return Duration.ofSeconds(seconds)
        }
        set(value) {
            cache.putLong(context.getString(R.string.pref_flashlight_timeout), value.seconds)
        }

}