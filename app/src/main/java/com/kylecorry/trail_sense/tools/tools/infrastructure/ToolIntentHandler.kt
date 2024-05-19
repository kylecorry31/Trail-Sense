package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Intent
import com.kylecorry.andromeda.fragments.AndromedaActivity

fun interface ToolIntentHandler {
    fun handle(activity: AndromedaActivity, intent: Intent): Boolean
}
