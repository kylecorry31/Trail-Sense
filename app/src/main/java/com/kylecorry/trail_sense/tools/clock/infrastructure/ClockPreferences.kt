package com.kylecorry.trail_sense.tools.clock.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class ClockPreferences(context: Context) : PreferenceRepo(context) {

    var showAnalogClock by BooleanPreference(
        cache,
        context.getString(R.string.pref_enable_analog_clock),
        true
    )

}
