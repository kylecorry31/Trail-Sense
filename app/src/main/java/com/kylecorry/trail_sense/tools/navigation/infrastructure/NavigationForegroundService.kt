package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.GPSPowerUsage
import com.kylecorry.andromeda.sense.location.LocationRequestConfig
import com.kylecorry.luna.time.FlowableTimer
import com.kylecorry.luna.time.ITimer
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import java.time.Duration
import java.time.ZonedDateTime

class NavigationForegroundService :
    IntervalService(wakelockDuration = Duration.ofSeconds(60), useOneTimeWorkers = true) {

    private val prefs by lazy { DependencyRegistry.get<UserPreferences>() }
    private val locationSubsystem by lazy { DependencyRegistry.get<LocationSubsystem>() }
    private val formatter by lazy { FormatService.getInstance(this) }
    private val navigationService = NavigationService()
    private val hikingService = HikingService()

    // Fixed until the live speed is available in the background
    private val hikingSpeed = navigationService.getHikingSpeed(0f)

    override val uniqueId: Int
        get() = 7238543

    override val period: Duration
        get() = prefs.navigation.backgroundUpdateInterval

    override val holdWakelockWhenBelowThreshold: Boolean
        get() = false

    // The GPS is only a wakeup source, the location is read in doWork
    override fun getNonWorkerTimer(action: suspend () -> Unit): ITimer {
        return FlowableTimer({ periodMillis ->
            GPS(
                this,
                LocationRequestConfig(
                    frequency = Duration.ofMillis(periodMillis),
                    powerUsage = prefs.gps.powerMode.powerUsage ?: GPSPowerUsage.High
                )
            )
        }, action = action)
    }

    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.navigation))
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return tryStartForegroundOrNotify {
            super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    override suspend fun doWork() {
        locationSubsystem.updateLocation()
        val notification = when (val destination = Navigator.getInstance(this).getDestination()) {
            is Destination.Path -> getPathNotification(destination)
            is Destination.Beacon -> getBeaconNotification(destination)
            is Destination.Bearing, null -> {
                stopSelf()
                return
            }
        }
        Notify.update(this, NOTIFICATION_ID, notification)
    }

    private fun getPathNotification(destination: Destination.Path): Notification {
        // The guidance is kept up to date by the path navigator when the location changes
        val guidance = destination.route.guidance
        val eta = hikingService.getHikingDuration(
            guidance.remainingDistance,
            guidance.remainingElevationGain,
            Speed.from(hikingSpeed, DistanceUnits.Meters, TimeUnits.Seconds)
        )
        return buildNotification(
            destination.path.name ?: getString(R.string.path),
            guidance.remainingDistance,
            eta,
            (guidance.progress * 100).toInt()
        )
    }

    private fun getBeaconNotification(destination: Destination.Beacon): Notification {
        val beacon = destination.beacon
        val location = locationSubsystem.location
        val eta = navigationService.eta(
            location,
            locationSubsystem.elevation.meters().value,
            hikingSpeed,
            beacon
        )
        return buildNotification(
            beacon.name.ifBlank { getString(R.string.navigation) },
            Distance.meters(location.distanceTo(beacon.coordinate)),
            eta
        )
    }

    private fun buildNotification(
        title: String,
        remainingDistance: Distance? = null,
        eta: Duration? = null,
        progressPercent: Int? = null
    ): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(formatRemaining(remainingDistance, eta))
            .setSmallIcon(R.drawable.ic_compass_icon)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(NavigationUtils.pendingIntent(this, R.id.action_navigation))
        if (progressPercent != null) {
            builder.setProgress(100, progressPercent, false)
        }
        return builder.build()
    }

    private fun formatRemaining(remainingDistance: Distance?, eta: Duration?): String? {
        if (remainingDistance == null || eta == null) {
            return null
        }
        val distance = remainingDistance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val arrival = formatter.formatTime(ZonedDateTime.now().plus(eta).toLocalTime(), includeSeconds = false)
        return "${formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)} · " +
                "${formatter.formatDuration(eta, false)} ($arrival)"
    }

    companion object {
        const val CHANNEL_ID = "Navigation_Background"
        private const val NOTIFICATION_ID = 578880

        fun start(context: Context) {
            tryStartForegroundOrNotify(context) {
                scheduler(context).start()
            }
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        private fun scheduler(context: Context): IAlwaysOnTaskScheduler {
            return TaskSchedulerFactory(context).alwaysOn(
                NavigationForegroundService::class.java,
                foreground = true
            )
        }
    }
}
