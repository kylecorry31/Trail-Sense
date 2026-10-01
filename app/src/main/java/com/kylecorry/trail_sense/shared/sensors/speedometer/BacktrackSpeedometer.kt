package com.kylecorry.trail_sense.shared.sensors.speedometer

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.specifications.LocationChangedSpecification
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant

class BacktrackSpeedometer(private val context: Context) : AbstractSensor(), ISpeedometer {

    private val pathService by lazy { PathService.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var path: LiveData<List<PathPoint>>? = null

    private var _speed: Speed? = null
    private var lastPointTime: Instant? = null

    private var pathObserver = Observer<List<PathPoint>> {
        val waypoints = it.sortedBy { point -> point.time }
        _speed = getSpeed(waypoints)
        lastPointTime = waypoints.lastOrNull()?.time
        notifyListeners()
    }

    override val hasValidReading: Boolean
        get() = prefs.paths.backtrackEnabled && _speed != null

    // Points are only recorded once the user has moved the minimum distance, so a missing point means they stopped
    override val speed: Speed
        get() {
            val stopped = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            val lastTime = lastPointTime ?: return _speed ?: stopped
            val staleAfter = prefs.paths.backtrackRecordFrequency.multipliedBy(2).plus(TIMING_BUFFER)
            return if (Duration.between(lastTime, Instant.now()) > staleAfter) {
                stopped
            } else {
                _speed ?: stopped
            }
        }

    private fun getSpeed(waypoints: List<PathPoint>): Speed? {
        return if (waypoints.size < 2) {
            null
        } else {
            val last = waypoints.last()
            val secondLast = waypoints[waypoints.size - 2]
            val distance = secondLast.coordinate.distanceTo(last.coordinate)

            val defaultError = Distance.meters(10f)

            val locationIsTheSame = LocationChangedSpecification(
                ApproximateCoordinate.from(
                    secondLast.coordinate,
                    defaultError
                ),
                prefs.odometerDistanceThreshold
            ).not()

            if (locationIsTheSame.isSatisfiedBy(
                    ApproximateCoordinate.from(
                        last.coordinate,
                        defaultError
                    )
                )
            ) {
                return Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            }

            val time = minOf(
                Duration.between(secondLast.time, last.time),
                prefs.paths.backtrackRecordFrequency.plus(TIMING_BUFFER)
            )
            Speed.from(
                distance / time.seconds.coerceAtLeast(1).toFloat(),
                DistanceUnits.Meters,
                TimeUnits.Seconds
            )
        }
    }

    override fun startImpl() {
        // TODO: Listen for path changes for backtrack ID
        val backtrack = runBlocking { pathService.getBacktrackPathId() } ?: return
        val waypoints = pathService.getWaypointsLive(backtrack)
        path = waypoints
        mainHandler.post { waypoints.observeForever(pathObserver) }
    }

    override fun stopImpl() {
        val waypoints = path ?: return
        path = null
        mainHandler.post { waypoints.removeObserver(pathObserver) }
    }

    private companion object {
        val TIMING_BUFFER: Duration = Duration.ofSeconds(30)
    }
}
