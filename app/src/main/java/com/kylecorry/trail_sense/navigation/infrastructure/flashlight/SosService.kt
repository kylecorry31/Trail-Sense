package com.kylecorry.trail_sense.navigation.infrastructure.flashlight

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import java.lang.Exception

class SosService : Service() {

    private var flashlight: IFlashlight? = null
    private var running = false
    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    private val code = listOf(
        MorseState.Dot, MorseState.Space, MorseState.Dot, MorseState.Space, MorseState.Dot,
        MorseState.Space,
        MorseState.Dash, MorseState.Space, MorseState.Dash, MorseState.Space, MorseState.Dash,
        MorseState.Space,
        MorseState.Dot, MorseState.Space, MorseState.Dot, MorseState.Space, MorseState.Dot,
        MorseState.WordSpace
    )

    private var codeIdx = 0

    private var runnable = Runnable {
        runNextState()
    }

    private fun runNextState() {
        if (!running) {
            codeIdx = 0
            flashlight?.off()
            return
        }

        codeIdx %= code.size
        val state = code[codeIdx]

        when (state) {
            MorseState.Dash, MorseState.Dot -> flashlight?.on()
            else -> flashlight?.off()
        }

        codeIdx++

        if (!running) {
            codeIdx = 0
            flashlight?.off()
            return
        }
        handler.postDelayed(runnable, getStateTime(state))
    }

    private fun getStateTime(state: MorseState): Long {
        return when (state) {
            MorseState.Dot -> 200
            MorseState.Dash -> 600
            MorseState.Space -> 200
            MorseState.LetterSpace -> 600
            MorseState.WordSpace -> 1400
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationUtils.createChannel(
            this,
            CHANNEL_ID,
            getString(R.string.flashlight_title),
            getString(R.string.flashlight_title),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.sos))
                .setContentText(getString(R.string.turn_off_flashlight))
                .setSmallIcon(R.drawable.flashlight_sos)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.sos))
                .setContentText(getString(R.string.turn_off_flashlight))
                .setSmallIcon(R.drawable.flashlight_sos)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY_COMPATIBILITY
    }

    override fun onCreate() {
        if (isOn(this)) {
            // Already on
            return
        }

        flashlight = Flashlight(this)
        running = true
        handler.post(runnable)
    }

    override fun onDestroy() {
        stopForeground(true)
        running = false
        handler.removeCallbacks(runnable)
        flashlight?.off()
        super.onDestroy()
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

        private enum class MorseState {
            Dot, Dash, Space, LetterSpace, WordSpace
        }

    }
}