package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight
import com.kylecorry.trailsensecore.infrastructure.services.ForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils

class FlashlightService: ForegroundService() {

    private var flashlight: IFlashlight? = null
    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return NotificationUtils.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.flashlight_title),
            getString(R.string.tap_to_turn_off),
            R.drawable.flashlight,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT
        )
    }

    override fun onDestroy() {
        flashlight?.off()
        isRunning = false
        stopService(true)
        super.onDestroy()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        flashlight = Flashlight(this)
        flashlight?.on()
        return START_STICKY_COMPATIBILITY
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 983589

        var isRunning = false
            private set

        fun intent(context: Context): Intent {
            return Intent(context, FlashlightService::class.java)
        }

        fun start(context: Context){
            try {
                ContextCompat.startForegroundService(context, intent(context))
            } catch (e: Exception){
                // Don't do anything
            }
        }

        fun stop(context: Context){
            context.stopService(intent(context))
        }
    }
}