package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.audio.ISoundPlayer
import com.kylecorry.trailsensecore.infrastructure.audio.PinkNoise
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.services.ForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.time.Instant

class WhiteNoiseService : ForegroundService() {

    private var whiteNoise: ISoundPlayer? = null
    private val cache by lazy { Cache(this) }

    private val offTimer = Intervalometer {
        stopSelf()
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        val stopAt = cache.getInstant(CACHE_KEY_OFF_TIME)
        if (stopAt != null && Instant.now() < stopAt){
            offTimer.once(Duration.between(Instant.now(), stopAt))
        }

        whiteNoise = MyPinkNoise()
        whiteNoise?.fadeOn()
        return START_STICKY_COMPATIBILITY
    }

    override fun getForegroundNotification(): Notification {
        return NotificationUtils.persistent(
            applicationContext,
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.tool_white_noise_title),
            getString(R.string.tap_to_turn_off),
            R.drawable.ic_tool_white_noise,
            intent = WhiteNoiseOffReceiver.pendingIntent(this)
        )
    }

    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun onDestroy() {
        isRunning = false
        whiteNoise?.fadeOff(true)
        stopService(true)
        offTimer.stop()
        cache.remove(CACHE_KEY_OFF_TIME)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 9874333
        const val NOTIFICATION_CHANNEL_ID = "white_noise"
        const val CACHE_KEY_OFF_TIME = "cache_white_noise_off_at"

        var isRunning = false
            private set

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
    }

}