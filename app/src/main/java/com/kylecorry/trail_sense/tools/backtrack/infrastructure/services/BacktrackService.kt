package com.kylecorry.trail_sense.tools.backtrack.infrastructure.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.kylecorry.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.Backtrack
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.infrastructure.services.CoroutineForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class BacktrackService : CoroutineForegroundService() {

    private val notify by lazy { Notify(this) }
    private val gps by lazy { sensorService.getGPS(true) }
    private val altimeter by lazy { sensorService.getAltimeter(true) }
    private val cellSignal by lazy { sensorService.getCellSignal(true) }
    private val sensorService by lazy { SensorService(this) }
    private val waypointRepo by lazy { WaypointRepo.getInstance(this) }
    private val beaconRepo by lazy { BeaconRepo.getInstance(this) }
    private val prefs by lazy { UserPreferences(applicationContext) }

    private val backtrack by lazy {
        Backtrack(
            this,
            gps,
            altimeter,
            cellSignal,
            waypointRepo,
            beaconRepo,
            prefs.backtrackSaveCellHistory,
            prefs.navigation.backtrackHistory
        )
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun getForegroundNotification(): Notification {
        return notify.background(
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            getString(R.string.backtrack_notification_channel),
            getString(R.string.backtrack_notification_description),
            R.drawable.ic_update
        )
    }

    override val foregroundNotificationId: Int = FOREGROUND_SERVICE_ID


    private fun scheduleNextUpdate() {
        val scheduler = BacktrackScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(prefs.backtrackRecordFrequency)
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    override suspend fun doWork() {
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        scheduleNextUpdate()
        backtrack.recordLocation()
        withContext(Dispatchers.Main) {
            stopService(true)
        }
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