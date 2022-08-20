package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.andromeda.torch.ITorch
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Instant

class StrobeService : ForegroundService() {

    private var torch: ITorch? = null
    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private var delay = 5L
    private val cache by lazy { Preferences(this) }
    private var on = false

    private val offTimer = Timer {
        val end = stopAt
        if (end != null && end <= Instant.now()){
            stopSelf()
        }
    }

    private var stopAt: Instant? = null

    private var runnable = Runnable {
        runNextState()
    }

    private val prefs by lazy { Preferences(applicationContext) }
    private val brightness by lazy { UserPreferences(applicationContext).flashlight.brightness }

    private fun runNextState() {
        if (!isRunning) {
            torch?.off()
            on = false
            return
        }

        if (on){
            torch?.off()
        } else {
            torch?.on(brightness)
        }

        on = !on

        handler.postDelayed(runnable, delay)
    }

    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return Notify.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.flashlight_strobe),
            getString(R.string.tap_to_turn_off),
            R.drawable.ic_strobe,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT,
            showForegroundImmediate = true
        )
    }

    override fun onDestroy() {
        offTimer.stop()
        isRunning = false
        handler.removeCallbacks(runnable)
        torch?.off()
        stopService(true)
        super.onDestroy()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        torch = Torch(this)
        isRunning = true
        delay = prefs.getLong(STROBE_DURATION_KEY) ?: 1000
        handler.post(runnable)
        stopAt = cache.getInstant(getString(R.string.pref_flashlight_timeout_instant))
        offTimer.interval(1000)
        return START_STICKY_COMPATIBILITY
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 763925
        const val STROBE_DURATION_KEY = "pref_flashlight_strobe_duration"

        var isRunning = false
            private set

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
    }
}