package com.kylecorry.trail_sense.tools.turn_back.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class TurnBackPreferences(context: Context) : PreferenceRepo(context) {
    val useAlarm by BooleanPreference(
        cache,
        getString(R.string.pref_turn_back_use_alarm),
        false
    )
}