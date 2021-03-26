package com.kylecorry.trail_sense.shared.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.kylecorry.trailsensecore.infrastructure.system.PowerUtils
import java.time.Duration

abstract class CustomService: Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        releaseWakelock()
        super.onDestroy()
    }

    private var wakelock: PowerManager.WakeLock? = null

    fun acquireWakelock(tag: String, duration: Duration){
        try {
            wakelock = PowerUtils.getWakelock(this, tag)
            releaseWakelock()
            wakelock?.acquire(duration.toMillis())
        } catch (e: Exception) {
            // DO NOTHING
        }
    }

    fun releaseWakelock() {
        try {
            if (wakelock?.isHeld == true) {
                wakelock?.release()
            }
        } catch (e: Exception) {
            // DO NOTHING
        }
    }

}