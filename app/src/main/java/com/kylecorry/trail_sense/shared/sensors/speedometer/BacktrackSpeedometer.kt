package com.kylecorry.trail_sense.shared.sensors.speedometer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.specifications.LocationChangedSpecification
import kotlinx.coroutines.runBlocking
import java.time.Duration

class BacktrackSpeedometer(private val context: Context) : AbstractSensor(), ISpeedometer {

    private val pathService by lazy { PathService.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private var path: LiveData<List<PathPoint>>? = null

    private var _speed: Speed? = null

    private var pathObserver = Observer<List<PathPoint>> {
        _speed = getSpeed(it.sortedBy { point -> point.time })
        notifyListeners()
    }

    override val hasValidReading: Boolean
        get() = prefs.backtrackEnabled && _speed != null

    override val speed: Speed
        get() = _speed ?: Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)

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
                return Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            }

            val time = Duration.between(secondLast.time, last.time)
            Speed(distance / time.seconds.toFloat(), DistanceUnits.Meters, TimeUnits.Seconds)
        }
    }

    override fun startImpl() {
        // TODO: Listen for path changes for backtrack ID
        val backtrack = runBlocking { pathService.getBacktrackPathId() } ?: return
        path = pathService.getWaypointsLive(backtrack)
        path?.observeForever(pathObserver)
    }

    override fun stopImpl() {
        path?.removeObserver(pathObserver)
    }
}