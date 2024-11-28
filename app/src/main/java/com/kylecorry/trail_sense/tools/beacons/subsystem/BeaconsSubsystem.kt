package com.kylecorry.trail_sense.tools.beacons.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.NearbyBeaconFilter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import kotlin.math.max

class BeaconsSubsystem(context: Context) {

    private val service = BeaconService(context)
    private val location = LocationSubsystem.getInstance(context)

    /**
     * Get nearby beacons. The max distance is specified through user preferences.
     * @param bounds: The bounds to search within. If null, the search will be in a geofence around the last known location with a radius of 1 kilometer.
     * @return A list of nearby beacons, sorted by distance ascending.
     */
    suspend fun getNearbyBeacons(bounds: CoordinateBounds? = null): List<Beacon> {
        val maxDistance = if (bounds == null) {
            Distance.kilometers(1f).meters().distance
        } else {
            max(bounds.height().meters().distance, bounds.width().meters().distance)
        }
        val searchBounds =
            bounds ?: CoordinateBounds.from(
                Geofence(
                    location.location,
                    Distance.meters(maxDistance)
                )
            )
        val beacons = service.getBeaconsInRegion(searchBounds)

        return NearbyBeaconFilter().filterNearbyBeacons(
            searchBounds.center,
            beacons,
            Int.MAX_VALUE,
            0f,
            maxDistance
        ).toList()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: BeaconsSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): BeaconsSubsystem {
            if (instance == null) {
                instance = BeaconsSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}