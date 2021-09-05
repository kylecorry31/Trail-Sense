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
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.specifications.LocationChangedSpecification
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import java.time.Duration
import java.time.Instant

class BacktrackSpeedometer(private val context: Context) : AbstractSensor(), ISpeedometer {

    private val backtrackRepo by lazy { WaypointRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private var waypointsLiveData: LiveData<List<WaypointEntity>>? = null
    private var waypoints = listOf<WaypointEntity>()

    private var waypointObserver = Observer<List<WaypointEntity>> {
        waypoints = it.filter { it.createdInstant > Instant.now().minus(prefs.navigation.backtrackHistory) }.sortedBy { it.createdInstant }
        notifyListeners()
    }

    override val hasValidReading: Boolean
        get() = prefs.backtrackEnabled && waypoints.size >= 2

    override val speed: Speed
        get() {
            // TODO: Store accuracy with waypoints
            return if (waypoints.size < 2) {
                Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
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

                val time = Duration.between(secondLast.createdInstant, last.createdInstant)
                Speed(distance / time.seconds.toFloat(), DistanceUnits.Meters, TimeUnits.Seconds)
            }
        }

    override fun startImpl() {
        waypointsLiveData = backtrackRepo.getWaypoints()
        waypointsLiveData?.observeForever(waypointObserver)
    }

    override fun stopImpl() {
        waypointsLiveData?.removeObserver(waypointObserver)
    }
}