package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem

class StopPedometerReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        PedometerSubsystem.getInstance(context).disable()
    }
}