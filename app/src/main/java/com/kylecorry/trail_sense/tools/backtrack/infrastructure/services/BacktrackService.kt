package com.kylecorry.trail_sense.tools.backtrack.infrastructure.services

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner
import com.kylecorry.trailsensecore.domain.network.CellSignal
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.services.CoroutineForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
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
    private val formatService by lazy { FormatServiceV2(this) }

    private val prefs by lazy { UserPreferences(applicationContext) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun getForegroundNotification(): Notification {
        return NotificationUtils.background(
            this,
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            getString(R.string.backtrack_notification_channel),
            getString(R.string.backtrack_notification_description),
            R.drawable.ic_update
        )
    }

    override val foregroundNotificationId: Int = FOREGROUND_SERVICE_ID

    private suspend fun getReadings() {
        withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
            val jobs = mutableListOf<Job>()
            jobs.add(launch { gps.read() })

            if (prefs.backtrackSaveCellHistory && PermissionUtils.isBackgroundLocationEnabled(
                    applicationContext
                )
            ) {
                jobs.add(launch { cellSignal.read() })
            }

            jobs.joinAll()
        }
        recordWaypoint()
        withContext(Dispatchers.Main) {
            stopService(true)
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
            createLastSignalBeacon(cell, gps.location, gps.altitude)
        }
    }

    private suspend fun createLastSignalBeacon(
        cellSignal: CellSignal?,
        location: Coordinate,
        altitude: Float
    ) {
        if (cellSignal == null) {
            return
        }
        val beaconRepo = BeaconRepo.getInstance(this)
        withContext(Dispatchers.IO) {
            val existing = beaconRepo.getTemporaryBeacon(BeaconOwner.CellSignal)
            beaconRepo.addBeacon(
                BeaconEntity.from(
                    Beacon(
                    existing?.id ?: 0L,
                    getString(
                        R.string.last_signal_beacon_name,
                        CellSignalUtils.getCellTypeString(
                            this@BacktrackService,
                            cellSignal.network
                        ),
                        formatService.formatQuality(cellSignal.quality)
                    ),
                    location,
                    false,
                    elevation = altitude,
                    temporary = true,
                    owner = BeaconOwner.CellSignal,
                    color = AppColor.Orange.color
                )
                )
            )
        }
    }


    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    override suspend fun doWork() {
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        scheduleNextUpdate()
        getReadings()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    companion object {

        private const val FOREGROUND_SERVICE_ID = 76984343
        private const val TAG = "BacktrackService"

        fun intent(context: Context): Intent {
            return Intent(context, BacktrackService::class.java)
        }

        fun start(context: Context) {
            IntentUtils.startService(context, intent(context), foreground = true)
        }
    }
}