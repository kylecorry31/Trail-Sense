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
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.asSignal
import com.kylecorry.trail_sense.shared.morse.Signal
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals
import java.time.Duration
import java.time.Instant

class SosService : ForegroundService() {

    private var torch: ITorch? = null
    private val signalPlayer by lazy {
        if (torch == null) null else SignalPlayer(
            torch!!.asSignal(
                UserPreferences(this).flashlight.brightness
            )
        )
    }
    private val cache by lazy { Preferences(this) }
    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    private val offTimer = Timer {
        val end = stopAt
        if (end != null && end <= Instant.now()) {
            stopSelf()
        }
    }

    private var stopAt: Instant? = null

    override fun getForegroundNotification(): Notification {
        return Notify.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.sos),
            getString(R.string.tap_to_turn_off),
            R.drawable.flashlight_sos,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT,
            showForegroundImmediate = true
        )
    }

    override fun onDestroy() {
        offTimer.stop()
        isRunning = false
        signalPlayer?.cancel()
        torch?.off()
        stopService(true)
        super.onDestroy()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        torch = Torch(this)
        val sos = Signals.sos(Duration.ofMillis(200)) + listOf(
            Signal.off(Duration.ofMillis(200L * 7))
        )
        signalPlayer?.play(sos, true)

        stopAt = cache.getInstant(getString(R.string.pref_flashlight_timeout_instant))
        offTimer.interval(1000)

        return START_STICKY_COMPATIBILITY
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 647354

        var isRunning = false
            private set

        fun intent(context: Context): Intent {
            return Intent(context, SosService::class.java)
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