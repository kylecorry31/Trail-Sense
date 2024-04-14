package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R

class ClockPreferences(context: Context) : PreferenceRepo(context) {

    var showAnalogClock by BooleanPreference(
        cache,
        context.getString(R.string.pref_enable_analog_clock),
        true
    )

}