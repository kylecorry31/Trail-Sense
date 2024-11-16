package com.kylecorry.trail_sense.tools.flashlight.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class FlashlightWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        FlashlightSubsystem.getInstance(context ?: return).toggle()
    }
}
