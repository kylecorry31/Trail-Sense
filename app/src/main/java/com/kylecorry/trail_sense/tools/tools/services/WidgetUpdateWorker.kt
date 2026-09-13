package com.kylecorry.trail_sense.tools.tools.services

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.PeriodicTaskSchedulerFactory
import com.kylecorry.andromeda.core.system.Wakelocks
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.widgets.Widgets
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.permissions.canGetLocationCustom
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem.SensorRefreshPolicy
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val wakelock = Wakelocks.get(applicationContext, TAG)
        wakelock?.acquire(Duration.ofSeconds(60).toMillis())
        val start = SystemClock.elapsedRealtime()
        try {
            // Update stale location/elevation data if needed
            tryOrLog {
                if (Tools.hasAnyWidgetsOnHomeScreen(applicationContext) { it.usesLocation }) {
                    updateStaleSensorData()
                }
            }

            // Update all widgets
            Tools.triggerWidgetUpdate(applicationContext, null)
        } finally {
            wakelock?.release()
        }
        getAppService<Logger>().info(TAG, "Widgets updated in ${SystemClock.elapsedRealtime() - start}ms")
        return Result.success()
    }

    private suspend fun updateStaleSensorData() {
        val locationSubsystem = LocationSubsystem.getInstance(applicationContext)
        val sensorSubsystem = SensorSubsystem.getInstance(applicationContext)

        if (!Permissions.canGetLocationCustom(applicationContext)) {
            return
        }

        val isLocationStale = locationSubsystem.locationAge.toMinutes() > 30
        val isElevationStale = locationSubsystem.elevationAge.toMinutes() > 30

        if (isLocationStale || isElevationStale) {
            getAppService<Logger>().info(
                TAG,
                "Refreshing stale widget sensor data (location age: ${locationSubsystem.locationAge.toMinutes()}min, " +
                    "elevation age: ${locationSubsystem.elevationAge.toMinutes()}min)"
            )
        }

        if (isLocationStale && isElevationStale) {
            sensorSubsystem.getLocationAndElevation(SensorRefreshPolicy.Refresh)
        } else if (isLocationStale) {
            sensorSubsystem.getLocation(SensorRefreshPolicy.Refresh)
        } else if (isElevationStale) {
            sensorSubsystem.getElevation(SensorRefreshPolicy.Refresh)
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val UNIQUE_ID = 267389

        val FREQUENCY: Duration = Duration.ofMinutes(30)

        fun start(context: Context) {
            scheduler(context).interval(FREQUENCY)
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        private fun scheduler(context: Context): IPeriodicTaskScheduler {
            return PeriodicTaskSchedulerFactory(context).deferrable(
                WidgetUpdateWorker::class.java,
                UNIQUE_ID
            )
        }
    }
}
