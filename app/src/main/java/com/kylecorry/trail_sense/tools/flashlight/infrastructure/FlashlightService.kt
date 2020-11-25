package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils

class FlashlightService: Service() {

    private var flashlight: IFlashlight? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationUtils.createChannel(this, CHANNEL_ID, getString(R.string.flashlight_title), getString(R.string.flashlight_title), NotificationUtils.CHANNEL_IMPORTANCE_LOW)

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.flashlight_title))
                .setContentText(getString(R.string.turn_off_flashlight))
                .setSmallIcon(R.drawable.flashlight)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.flashlight_title))
                .setContentText(getString(R.string.turn_off_flashlight))
                .setSmallIcon(R.drawable.flashlight)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY_COMPATIBILITY
    }

    override fun onCreate() {
        if (isOn(this)){
            // Already on
            return
        }

        flashlight = Flashlight(this)
        flashlight?.on()
    }

    override fun onDestroy() {
        stopForeground(true)
        flashlight?.off()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 983589

        fun intent(context: Context): Intent {
            return Intent(context, FlashlightService::class.java)
        }

        fun start(context: Context){
            try {
                ContextCompat.startForegroundService(context, intent(context))
            } catch (e: Exception){
                // Don't do anything
            }
        }

        fun stop(context: Context){
            context.stopService(intent(context))
        }

        fun isOn(context: Context): Boolean {
            return NotificationUtils.isNotificationActive(context, NOTIFICATION_ID)
        }
    }
}