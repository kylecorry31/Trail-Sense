package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.audio.ISoundPlayer
import com.kylecorry.trailsensecore.infrastructure.audio.WhiteNoise
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils

class WhiteNoiseService : Service() {

    private var whiteNoise: ISoundPlayer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationUtils.builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.tool_white_noise_title))
            .setContentText(getString(R.string.tap_to_turn_off))
            .setSmallIcon(R.drawable.ic_tool_white_noise)
            .setContentIntent(WhiteNoiseOffReceiver.pendingIntent(this))
            .build()

        startForeground(NOTIFICATION_ID, notification)
        whiteNoise = WhiteNoise()
        whiteNoise?.on()
        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        super.onDestroy()
        whiteNoise?.off()
        whiteNoise?.release()
    }

    companion object {
        const val NOTIFICATION_ID = 9874333
        const val NOTIFICATION_CHANNEL_ID = "white_noise"

        fun intent(context: Context): Intent {
            return Intent(context, WhiteNoiseService::class.java)
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