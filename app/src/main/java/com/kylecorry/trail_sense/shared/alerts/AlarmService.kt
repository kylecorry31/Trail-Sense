package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.background.services.AndromedaService
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import java.time.Duration

class AlarmService : AndromedaService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notificationChannel = intent?.getStringExtra(EXTRA_NOTIFICATION_CHANNEL)
        if (notificationChannel == null) {
            stopService(true)
            return START_NOT_STICKY
        }

        acquireWakelock("AlarmService", Duration.ofMillis(WAKE_LOCK_TIMEOUT_MS))

        AlarmPlayer(this, notificationChannel) { stopService(true) }.play()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    override fun getForegroundInfo() = ForegroundInfo(
        NOTIFICATION_ID,
        Notify.background(
            this,
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.alarm_notification_title),
            null,
            R.drawable.ic_alert
        ),
        listOf(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    )

    companion object {
        private const val EXTRA_NOTIFICATION_CHANNEL = "notification_channel"
        private const val WAKE_LOCK_TIMEOUT_MS = 7000L
        private const val NOTIFICATION_ID = 2390424
        const val NOTIFICATION_CHANNEL_ID = "alarm_service"

        fun start(context: Context, notificationChannel: String) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_CHANNEL, notificationChannel)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
