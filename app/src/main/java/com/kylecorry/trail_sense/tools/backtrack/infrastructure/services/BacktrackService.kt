package com.kylecorry.trail_sense.tools.backtrack.infrastructure.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.services.CoroutineForegroundService
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class BacktrackService : CoroutineForegroundService() {

    private val gps by lazy { sensorService.getGPS(true) }
    private val cellSignal by lazy { sensorService.getCellSignal(true) }
    private val sensorService by lazy { SensorService(applicationContext) }
    private val waypointRepo by lazy { WaypointRepo.getInstance(applicationContext) }

    private val prefs by lazy { UserPreferences(applicationContext) }
    private val cache by lazy { Cache(applicationContext) }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun getForegroundNotification(): Notification {
        return notification(
            getString(R.string.backtrack_notification_channel),
            getString(R.string.backtrack_notification_description),
            R.drawable.ic_update
        )
    }

    override val foregroundNotificationId: Int = FOREGROUND_SERVICE_ID

    private fun getReadings() {
        serviceScope.launch {
            withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
                val jobs = mutableListOf<Job>()
                if (!gps.hasValidReading) {
                    jobs.add(launch { gps.read() })
                }

                if (!cellSignal.hasValidReading || !prefs.backtrackSaveCellHistory ||
                    !PermissionUtils.isBackgroundLocationEnabled(applicationContext)
                ) {
                    jobs.add(launch { cellSignal.read() })
                }

                jobs.joinAll()
            }
            cache.putLong(CACHE_LAST_LOCATION_UPDATE, Instant.now().toEpochMilli())
            recordWaypoint()
            withContext(Dispatchers.Main) {
                stopService(true)
            }
        }
    }

    private fun scheduleNextUpdate() {
        val scheduler = BacktrackScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(prefs.backtrackRecordFrequency)
    }


    private suspend fun recordWaypoint() {
        withContext(Dispatchers.IO) {
            val cell = cellSignal.signals.maxByOrNull { it.strength }
            waypointRepo.addWaypoint(
                WaypointEntity(
                    gps.location.latitude,
                    gps.location.longitude,
                    gps.altitude,
                    Instant.now().toEpochMilli(),
                    cell?.network?.id,
                    cell?.quality?.ordinal,
                )
            )
            waypointRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
        }
    }


    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    override suspend fun doWork() {
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        scheduleNextUpdate()

        val timeSinceLast =
            Instant.now().toEpochMilli() - (cache.getLong(CACHE_LAST_LOCATION_UPDATE)
                ?: 0L)

        if (timeSinceLast > Duration.ofMinutes(5).toMillis() || timeSinceLast < 0) {
            getReadings()
        } else {
            stopSelf()
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun notification(title: String, content: String, @DrawableRes icon: Int): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, FOREGROUND_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(icon)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(false)
                .setGroup(NotificationChannels.GROUP_UPDATES)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(icon)
                .setPriority(Notification.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(false)
                .setGroup(NotificationChannels.GROUP_UPDATES)
                .build()
        }
    }


    companion object {

        private const val FOREGROUND_SERVICE_ID = 76984343
        const val FOREGROUND_CHANNEL_ID = "Backtrack"
        private const val TAG = "BacktrackService"
        const val CACHE_LAST_LOCATION_UPDATE = "cache_last_backtrack_time"

        fun intent(context: Context): Intent {
            return Intent(context, BacktrackService::class.java)
        }

        fun start(context: Context) {
            IntentUtils.startService(context, intent(context), foreground = true)
        }
    }
}