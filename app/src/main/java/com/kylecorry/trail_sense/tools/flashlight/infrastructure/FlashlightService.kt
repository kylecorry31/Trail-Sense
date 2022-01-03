package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.andromeda.torch.ITorch
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import java.time.Instant

class FlashlightService: ForegroundService() {

    private var torch: ITorch? = null
    private val cache by lazy { Preferences(this) }

    private val timer = Timer {
        torch?.on()
    }

    private val offTimer = Timer {
        val end = stopAt
        if (end != null && end <= Instant.now()){
            stopSelf()
        }
    }

    private var stopAt: Instant? = null

    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return Notify.persistent(
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
        timer.stop()
        torch?.off()
        offTimer.stop()
        isRunning = false
        stopService(true)
        super.onDestroy()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        torch = Torch(this)
        timer.interval(200)
        stopAt = cache.getInstant(getString(R.string.pref_flashlight_timeout_instant))
        offTimer.interval(1000)
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