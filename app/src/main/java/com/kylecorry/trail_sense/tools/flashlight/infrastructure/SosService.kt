package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.morse.MorseService
import com.kylecorry.trailsensecore.domain.morse.Signal
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight
import com.kylecorry.trailsensecore.infrastructure.morse.SignalPlayer
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import java.lang.Exception
import java.time.Duration

class SosService : Service() {

    private var flashlight: IFlashlight? = null
    private var running = false
    private val signalPlayer by lazy { if (flashlight == null) null else SignalPlayer(flashlight!!) }
    private val morseService = MorseService()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationUtils.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.sos),
            getString(R.string.tap_to_turn_off),
            R.drawable.flashlight_sos,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT
        )

        startForeground(NOTIFICATION_ID, notification)

        flashlight = Flashlight(this)
        running = true
        val sos = morseService.sosSignal(Duration.ofMillis(200)) + listOf(
            Signal.off(Duration.ofMillis(200 * 7))
        )
        signalPlayer?.play(sos, true)
        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        running = false
        signalPlayer?.cancel()
        flashlight?.off()
        super.onDestroy()
        stopForeground(true)
        stopSelf()
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 647354

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

        fun isOn(context: Context): Boolean {
            return NotificationUtils.isNotificationActive(context, NOTIFICATION_ID)
        }
    }
}