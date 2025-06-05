package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class WaterBoilTimerPreferences(context: Context) : PreferenceRepo(context) {

    val useAlarm: Boolean
        get() = false

}