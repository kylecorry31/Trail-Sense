package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.andromeda.torch.ITorch
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.asSignal
import com.kylecorry.trailsensecore.domain.morse.MorseService
import com.kylecorry.trailsensecore.domain.morse.Signal
import com.kylecorry.trailsensecore.infrastructure.morse.SignalPlayer
import java.time.Duration

class SosService : ForegroundService() {

    private var torch: ITorch? = null
    private val signalPlayer by lazy { if (torch == null) null else SignalPlayer(torch!!.asSignal()) }
    private val morseService = MorseService()
    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return Notify.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.sos),
            getString(R.string.tap_to_turn_off),
            R.drawable.flashlight_sos,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT
        )
    }

    override fun onDestroy() {
        isRunning = false
        signalPlayer?.cancel()
        torch?.off()
        stopService(true)
        super.onDestroy()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        torch = Torch(this)
        val sos = morseService.sosSignal(Duration.ofMillis(200)) + listOf(
            Signal.off(Duration.ofMillis(200L * 7))
        )
        signalPlayer?.play(sos, true)
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