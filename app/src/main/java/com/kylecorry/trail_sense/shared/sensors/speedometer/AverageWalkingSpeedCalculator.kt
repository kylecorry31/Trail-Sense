package com.kylecorry.trail_sense.shared.sensors.speedometer

import com.kylecorry.andromeda.core.time.IZonedDateTimeProvider
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import java.time.Duration

class AverageWalkingSpeedCalculator(
    private val paceCalculator: IPaceCalculator,
    private val stepTrackerService: IStepTrackerService,
    private val pathService: IPathService,
    private val timeProvider: IZonedDateTimeProvider,
    private val isPedometerEnabled: Boolean
) {

    suspend fun getAverageWalkingSpeed(): Speed? {
        val currentPedometerSession = if (isPedometerEnabled) {
            stepTrackerService.getOpenStepTrackingPeriod()
        } else {
            null
        }
        val currentPedometerSessionSpeed = currentPedometerSession?.getSpeed(AveragePaceTimeMode.Active, paceCalculator)
        if (currentPedometerSessionSpeed != null && isValidSession(
                currentPedometerSessionSpeed,
                currentPedometerSession.activeTime
            )
        ) {
            return currentPedometerSessionSpeed
        }

        val currentBacktrackSession = pathService.getBacktrackPathId()?.let { pathService.getPath(it) }
        val backtrackDuration = currentBacktrackSession?.metadata?.getDuration()
        val backtrackSpeed = backtrackDuration?.let {
            Speed.from(
                currentBacktrackSession.metadata.distance.meters().value / backtrackDuration.seconds,
                DistanceUnits.Meters,
                TimeUnits.Seconds
            )
        }
        if (backtrackSpeed != null && isValidSession(backtrackSpeed, backtrackDuration)) {
            return backtrackSpeed
        }

        val pastPedometerSessions = stepTrackerService.getAllStepTrackingPeriods().filter {
            it.endTime != null && it.endTime.isAfter(
                timeProvider.getTime().minus(MAXIMUM_PEDOMETER_SESSION_AGE_FOR_SPEED).toInstant()
            ) && isValidSession(
                it.getSpeed(AveragePaceTimeMode.Active, paceCalculator),
                it.activeTime
            )
        }

        if (pastPedometerSessions.isNotEmpty()) {
            val averageSpeed = Statistics.median(pastPedometerSessions.map {
                it.getSpeed(AveragePaceTimeMode.Active, paceCalculator).convertTo(
                    DistanceUnits.Meters,
                    TimeUnits.Seconds
                ).value
            })
            return Speed.from(averageSpeed, DistanceUnits.Meters, TimeUnits.Seconds)
        }

        return null
    }

    private fun isValidSession(speed: Speed, duration: Duration): Boolean {
        val speedMetersPerSecond = speed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).value
        return speedMetersPerSecond in MINIMUM_SPEED_METERS_PER_SECOND..MAXIMUM_SPEED_METERS_PER_SECOND && duration > MINIMUM_SESSION_DURATION
    }

    companion object {
        private val MINIMUM_SESSION_DURATION = Duration.ofMinutes(15)
        private val MAXIMUM_PEDOMETER_SESSION_AGE_FOR_SPEED = Duration.ofDays(30)
        private const val MINIMUM_SPEED_METERS_PER_SECOND = 0.3f
        private const val MAXIMUM_SPEED_METERS_PER_SECOND = 2.0f
    }

}
