package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.background.services.AndromedaService
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.withId
import java.time.Duration
import java.time.Instant

class WhiteNoiseService : AndromedaService() {

    private var soundPlayer: ISoundPlayer? = null
    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }

    private val offTimer = CoroutineTimer {
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isRunning = true
        val stopAt = cache.getInstant(CACHE_KEY_OFF_TIME)
        if (stopAt != null && Instant.now() < stopAt) {
            offTimer.once(Duration.between(Instant.now(), stopAt))
        }

        val soundId = cache.getLong(CACHE_KEY_SLEEP_SOUND_ID) ?: SleepSound.PinkNoise.id
        val sound = SleepSound.entries.withId(soundId) ?: SleepSound.PinkNoise

        soundPlayer = SleepSoundFactory().getSleepSound(sound)
        soundPlayer?.fadeOn()
        return START_STICKY
    }

    override fun getForegroundInfo(): ForegroundInfo {
        val notification = Notify.persistent(
            this,
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.tool_white_noise_title),
            getString(R.string.tap_to_turn_off),
            R.drawable.ic_tool_white_noise,
            intent = WhiteNoiseOffReceiver.pendingIntent(this),
            showForegroundImmediate = true
        )
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        offTimer.stop()
        isRunning = false
        soundPlayer?.fadeOff(true)
        stopService(true)
        clearSleepTimer(this)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 9874333
        const val NOTIFICATION_CHANNEL_ID = "white_noise"
        const val CACHE_KEY_OFF_TIME = "cache_white_noise_off_at"

        const val CACHE_KEY_SLEEP_SOUND_ID = "cache_sleep_sound_id"

        var isRunning = false
            private set

        fun intent(context: Context): Intent {
            return Intent(context, WhiteNoiseService::class.java)
        }

        fun play(
            context: Context,
            sleepSound: SleepSound? = null,
            duration: Duration? = null
        ) {
            val cache = AppServiceRegistry.get<PreferencesSubsystem>().preferences
            if (duration != null && !duration.isZero) {
                cache.putInstant(CACHE_KEY_OFF_TIME, Instant.now().plus(duration))
            } else {
                clearSleepTimer(context)
            }

            // Allow the last selected sound to be played
            if (sleepSound != null) {
                cache.putLong(CACHE_KEY_SLEEP_SOUND_ID, sleepSound.id)
            }
            start(context)
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

        fun clearSleepTimer(context: Context) {
            PreferencesSubsystem.getInstance(context).preferences.remove(CACHE_KEY_OFF_TIME)
        }
    }

}