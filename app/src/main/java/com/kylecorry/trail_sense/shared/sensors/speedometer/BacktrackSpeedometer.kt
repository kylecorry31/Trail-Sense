package com.kylecorry.trail_sense.shared.sensors.speedometer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.Speed
import com.kylecorry.trailsensecore.domain.units.TimeUnits
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.speedometer.ISpeedometer
import java.time.Duration

class BacktrackSpeedometer(private val context: Context): AbstractSensor(), ISpeedometer {

    private val backtrackRepo by lazy { WaypointRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private var waypointsLiveData: LiveData<List<WaypointEntity>>? = null
    private var waypoints = listOf<WaypointEntity>()

    private var waypointObserver = Observer<List<WaypointEntity>> {
        waypoints = it.sortedBy { it.createdInstant }
        notifyListeners()
    }

    override val hasValidReading: Boolean
        get() = prefs.backtrackEnabled && waypoints.size >= 2

    override val speed: Speed
        get() {
            return if (waypoints.size < 2){
                Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            } else {
                val last = waypoints.last()
                val secondLast = waypoints[waypoints.size - 2]
                val distance = secondLast.coordinate.distanceTo(last.coordinate)

                if (distance <= prefs.odometerDistanceThreshold.meters().distance){
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