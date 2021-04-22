package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight
import com.kylecorry.trailsensecore.infrastructure.services.ForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import java.lang.Exception

class StrobeService : ForegroundService() {

    private var flashlight: IFlashlight? = null
    private var running = false
    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private var on = false

    private var runnable = Runnable {
        runNextState()
    }

    private fun runNextState() {
        if (!running) {
            flashlight?.off()
            on = false
            return
        }

        if (on){
            flashlight?.off()
        } else {
            flashlight?.on()
        }

        on = !on

        handler.postDelayed(runnable, STROBE_DELAY)
    }

    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return NotificationUtils.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.flashlight_strobe),
            getString(R.string.tap_to_turn_off),
            R.drawable.ic_strobe,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT
        )
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(runnable)
        flashlight?.off()
        stopService(true)
        super.onDestroy()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        flashlight = Flashlight(this)
        running = true
        handler.post(runnable)
        return START_STICKY_COMPATIBILITY
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 763925
        private const val STROBE_DELAY = 5L

        fun intent(context: Context): Intent {
            return Intent(context, StrobeService::class.java)
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, intent(context))
            } catch (e: Exception) {
                // Don't do anything
            }
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

        fun isOn(context: Context): Boolean {
            return NotificationUtils.isNotificationActive(context, NOTIFICATION_ID)
        }
    }
}