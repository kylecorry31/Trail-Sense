package com.kylecorry.trail_sense.navigation.infrastructure.flashlight

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.system.NotificationUtils

class FlashlightService: Service() {

    lateinit var flashlight: Flashlight

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY_COMPATIBILITY
    }

    override fun onCreate() {
        if (isOn(this)){
            // Already on
            return
        }

        NotificationUtils.createChannel(this, CHANNEL_ID, "Flashlight", "Flashlight", NotificationUtils.CHANNEL_IMPORTANCE_LOW)

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Flashlight")
                .setContentText("Tap to turn off")
                .setSmallIcon(R.drawable.flashlight)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Flashlight")
                .setContentText("Tap to turn off")
                .setSmallIcon(R.drawable.flashlight)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)

        flashlight = Flashlight(this)
        flashlight.on()
    }

    override fun onDestroy() {
        stopForeground(true)
        flashlight.off()
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