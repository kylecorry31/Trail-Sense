package com.kylecorry.trail_sense.tools.turn_back.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class TurnBackPreferences(context: Context) : PreferenceRepo(context) {
    val useAlarm: Boolean
        get() = false
}