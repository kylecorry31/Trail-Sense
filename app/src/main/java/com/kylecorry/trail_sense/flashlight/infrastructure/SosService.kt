package com.kylecorry.trail_sense.flashlight.infrastructure

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.system.NotificationUtils
import java.lang.Exception
import kotlin.concurrent.thread

class SosService: Service() {

    lateinit var flashlight: Flashlight
    lateinit var thread: Thread
    var running = false

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
                .setContentTitle("SOS")
                .setContentText("Tap to turn off")
                .setSmallIcon(R.drawable.flashlight)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("SOS")
                .setContentText("Tap to turn off")
                .setSmallIcon(R.drawable.flashlight)
                .setContentIntent(FlashlightOffReceiver.pendingIntent(this))
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)

        flashlight = Flashlight(this)

        running = true

        thread = thread {
            while (running){
                try {
                    dot()
                    space()
                    dot()
                    space()
                    dot()
                    letterSpace()
                    dash()
                    space()
                    dash()
                    space()
                    dash()
                    letterSpace()
                    dot()
                    space()
                    dot()
                    space()
                    dot()
                    Thread.sleep(1400)
                } catch (e: Exception){
                    // Ignore
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        thread.interrupt()
        flashlight.off()
        NotificationUtils.cancel(this, NOTIFICATION_ID)
    }

    private fun dot(){
        if (!running){
            return
        }
        flashlight.on()
        Thread.sleep(200)
        flashlight.off()
    }

    private fun letterSpace(){
        if (!running){
            return
        }
        Thread.sleep(600)
    }

    private fun space(){
        if (!running){
            return
        }
        Thread.sleep(200)
    }

    private fun dash(){
        if (!running){
            return
        }
        flashlight.on()
        Thread.sleep(600)
        flashlight.off()
    }

    companion object {
        const val CHANNEL_ID = "Flashlight"
        const val NOTIFICATION_ID = 647354

        fun intent(context: Context): Intent {
            return Intent(context, SosService::class.java)
        }

        fun start(context: Context){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent(context))
            } else {
                context.startService(intent(context))
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