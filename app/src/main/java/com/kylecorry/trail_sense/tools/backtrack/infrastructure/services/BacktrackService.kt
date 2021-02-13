package com.kylecorry.trail_sense.tools.backtrack.infrastructure.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.PowerUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.network.CellSignalSensor
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class BacktrackService : Service() {

    private val gps by lazy { sensorService.getGPS(true) }
    private val cellSignal by lazy { CellSignalSensor(applicationContext) }
    private val sensorService by lazy { SensorService(applicationContext) }
    private val waypointRepo by lazy { WaypointRepo.getInstance(applicationContext) }
    private var wakelock: PowerManager.WakeLock? = null

    private val prefs by lazy { UserPreferences(applicationContext) }
    private val cache by lazy { Cache(applicationContext) }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notification(
            getString(R.string.backtrack_notification_channel),
            getString(R.string.backtrack_notification_description),
            R.drawable.ic_update
        )
        startForeground(FOREGROUND_SERVICE_ID, notification)

        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        acquireWakelock()
        scheduleNextUpdate()

        val timeSinceLast =
            Instant.now().toEpochMilli() - (cache.getLong(CACHE_LAST_LOCATION_UPDATE)
                ?: 0L)

        if (timeSinceLast > Duration.ofMinutes(5).toMillis() || timeSinceLast < 0) {
            getReadings()
        } else {
            wrapUp()
        }

        return START_NOT_STICKY
    }

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
                wrapUp()
            }
        }
    }

    private fun scheduleNextUpdate() {
        val scheduler = BacktrackScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(prefs.backtrackRecordFrequency)
    }

    private fun releaseWakelock() {
        try {
            if (wakelock?.isHeld == true) {
                wakelock?.release()
            }
        } catch (e: Exception) {
            // DO NOTHING
        }
    }

    private fun acquireWakelock() {
        try {
            wakelock = PowerUtils.getWakelock(applicationContext, TAG)
            releaseWakelock()
            wakelock?.acquire(60 * 1000L)
        } catch (e: Exception) {
            // DO NOTHING
        }
    }

    private suspend fun recordWaypoint() {
        withContext(Dispatchers.IO) {
            waypointRepo.addWaypoint(
                WaypointEntity(
                    gps.location.latitude,
                    gps.location.longitude,
                    gps.altitude,
                    gps.time.toEpochMilli(),
                    cellSignal.signals.firstOrNull()?.network?.id,
                    cellSignal.signals.firstOrNull()?.quality?.ordinal,
                )
            )
            waypointRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
        }
    }


    override fun onDestroy() {
        wrapUp()
        super.onDestroy()
    }

    private fun wrapUp() {
        serviceJob.cancel()
        releaseWakelock()
        stopForeground(true)
        stopSelf()
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