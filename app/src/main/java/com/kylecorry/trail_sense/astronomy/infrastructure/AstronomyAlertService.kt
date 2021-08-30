package com.kylecorry.trail_sense.astronomy.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.CoroutineForegroundService
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.LunarEclipseAlertCommand
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.MeteorShowerAlertCommand
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.TestAlertCommand
import com.kylecorry.trail_sense.shared.commands.LocationCommand
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.LocalDateTime

class AstronomyAlertService : CoroutineForegroundService() {

    private val gps by lazy { SensorService(this).getGPS(true) }

    override val foregroundNotificationId: Int
        get() = 237041234

    override suspend fun doWork() {
        acquireWakelock(TAG, Duration.ofSeconds(30))
        val now = LocalDateTime.now()
        Log.i(TAG, "Broadcast received at $now")

        withContext(Dispatchers.IO) {
            withTimeoutOrNull(Duration.ofSeconds(12).toMillis()) {
                if (!gps.hasValidReading) {
                    gps.read()
                }
            }
        }

        val location = gps.location

        if (location == Coordinate.zero) {
            return
        }

        val commands: List<LocationCommand> = listOf(
            LunarEclipseAlertCommand(this),
            MeteorShowerAlertCommand(this),
            TestAlertCommand(this)
        )

        withContext(Dispatchers.Main) {
            commands.forEach { it.execute(location) }
            stopService(true)
        }

    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }


    override fun getForegroundNotification(): Notification {
        return Notify.background(
            this,
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            getString(R.string.background_update),
            getString(R.string.checking_for_astronomy_events),
            R.drawable.ic_update
        )
    }

    companion object {

        const val TAG = "AstronomyAlertService"
        const val NOTIFICATION_ID = 687432

        fun intent(context: Context): Intent {
            return Intent(context, AstronomyAlertService::class.java)
        }
    }

}