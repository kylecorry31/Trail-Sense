package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.services.AndromedaService
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import java.time.Duration
import java.time.Instant

class FlashlightService : AndromedaService() {

    private val flashlight by lazy { FlashlightSubsystem.getInstance(this) }
    private val cache by lazy { Preferences(this) }

    private var strategy: IFlashlightStrategy? = null

    private val topic by lazy { flashlight.mode.replay() }

    private val offTimer = Timer {
        val end = stopAt
        if (end != null && end <= Instant.now() && flashlight.getMode() != FlashlightMode.Off) {
            flashlight.set(FlashlightMode.Off)
        }
    }

    private var stopAt: Instant? = null

    private fun getNotification(): Notification {
        return Notify.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.flashlight_title),
            getString(R.string.tap_to_turn_off),
            R.drawable.flashlight,
            intent = FlashlightOffReceiver.pendingIntent(this),
            group = NotificationChannels.GROUP_FLASHLIGHT,
            showForegroundImmediate = true
        )
    }

    override fun onDestroy() {
        topic.unsubscribe(this::onStateChanged)
        offTimer.stop()
        strategy?.stop()
        Notify.cancel(this, NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notify.send(this, NOTIFICATION_ID, getNotification())
        topic.subscribe(this::onStateChanged)
        stopAt = cache.getInstant(getString(R.string.pref_flashlight_timeout_instant))
        offTimer.interval(1000)
        return START_STICKY_COMPATIBILITY
    }

    private fun onStateChanged(mode: FlashlightMode): Boolean {
        when (mode) {
            FlashlightMode.Torch -> switchStrategy(TorchFlashlightStrategy(flashlight))
            FlashlightMode.Sos -> switchStrategy(SosFlashlightStrategy(flashlight))
            FlashlightMode.Off -> switchStrategy(null)
            else -> switchStrategy(StrobeFlashlightStrategy(flashlight, getInterval(mode)))
        }
        return true
    }

    private fun getInterval(mode: FlashlightMode): Duration {
        val frequency = when (mode) {
            FlashlightMode.Off -> 1
            FlashlightMode.Torch -> 1
            FlashlightMode.Strobe1 -> 1
            FlashlightMode.Strobe2 -> 2
            FlashlightMode.Strobe3 -> 3
            FlashlightMode.Strobe4 -> 4
            FlashlightMode.Strobe5 -> 5
            FlashlightMode.Strobe6 -> 6
            FlashlightMode.Strobe7 -> 7
            FlashlightMode.Strobe8 -> 8
            FlashlightMode.Strobe9 -> 9
            FlashlightMode.Strobe200 -> 200
            FlashlightMode.Sos -> 1
        }
        return Duration.ofMillis(1000L / frequency)
    }

    private fun switchStrategy(newStrategy: IFlashlightStrategy?) {
        strategy?.stop()
        strategy = newStrategy
        strategy?.start()
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 983589

        fun intent(context: Context): Intent {
            return Intent(context, FlashlightService::class.java)
        }

        fun start(context: Context) {
            Intents.startService(context, intent(context), false)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
            Notify.cancel(context, NOTIFICATION_ID)
        }
    }
}