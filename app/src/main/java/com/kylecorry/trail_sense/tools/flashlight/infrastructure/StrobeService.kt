package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import java.lang.Exception

class StrobeService : Service() {

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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY_COMPATIBILITY
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createChannel(
            this,
            CHANNEL_ID,
            getString(R.string.flashlight_title),
            getString(R.string.flashlight_title),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW
        )

        val notification = NotificationUtils.builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.flashlight_strobe))
            .setContentText(getString(R.string.turn_off_flashlight))
            .setSmallIcon(R.drawable.flashlight)
            .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
            .build()

        startForeground(NOTIFICATION_ID, notification)
        flashlight = Flashlight(this)
        running = true
        handler.post(runnable)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(runnable)
        flashlight?.off()
        super.onDestroy()
        stopForeground(true)
        stopSelf()
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