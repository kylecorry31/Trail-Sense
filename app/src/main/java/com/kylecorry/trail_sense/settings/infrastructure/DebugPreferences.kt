package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.shared.debugging.isDebug

class DebugPreferences(context: Context) : PreferenceRepo(context) {

    private var isTapMapToSetLocationEnabled by BooleanPreference(
        cache,
        TAP_MAP_TO_SET_LOCATION_KEY,
        false
    )

    var tapMapToSetLocation: Boolean
        get() = isDebug() && isTapMapToSetLocationEnabled
        set(value) {
            isTapMapToSetLocationEnabled = value
        }

    private companion object {
        const val TAP_MAP_TO_SET_LOCATION_KEY = "pref_debug_tap_map_to_set_location"
    }
}
