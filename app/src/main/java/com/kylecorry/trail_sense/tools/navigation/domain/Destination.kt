package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.GuidanceInput
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.GuidanceProvider
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRoute
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.absoluteValue

sealed class Destination : GuidanceProvider {
    class Path(
        val path: com.kylecorry.trail_sense.tools.paths.domain.Path,
        points: List<PathPoint>
    ) : Destination() {
        val route = PathRoute(points)
        override fun getGuidance(input: GuidanceInput): Guidance {
            val guidance = route.guidance
            val vector = Geography.navigate(input.location, guidance.target, input.declination, input.useTrueNorth)
            return Guidance(
                targetBearing = vector.direction,
                remainingDistance = guidance.remainingDistance,
                traveledDistance = guidance.distanceAlong,
                offRouteDistance = guidance.offRouteDistance,
                remainingElevationGain = guidance.remainingElevationGain,
                remainingElevationLoss = guidance.remainingElevationLoss,
                progress = guidance.progress,
                arrived = guidance.arrived
            )
        }
    }

    data class Bearing(
        val bearing: com.kylecorry.sol.units.Bearing,
        val isTrueNorth: Boolean,
        val declination: Float,
        val startingLocation: Coordinate? = null,
        val bearingDistance: Distance = defaultBearingDistance
    ) : Destination() {

        val trueBearing: com.kylecorry.sol.units.Bearing
            get() {
                return if (isTrueNorth) {
                    bearing
                } else {
                    bearing.withDeclination(declination)
                }
            }

        val targetLocation: Coordinate?
            get() {
                return startingLocation?.plus(bearingDistance, trueBearing)
            }

        override fun getGuidance(input: GuidanceInput): Guidance {
            val target = targetLocation
            val vector = target?.let { Geography.navigate(input.location, it, input.declination, input.useTrueNorth) }
            val traveledDistance = startingLocation?.let { Distance.meters(it.distanceTo(input.location)) }
            val offRouteDistance = if (target != null && startingLocation != null) {
                Geography.getCrossTrackDistance(
                    input.location,
                    startingLocation,
                    target
                ).let {
                    Distance.from(it.value.absoluteValue, it.units)
                }
            } else {
                null
            }

            return Guidance(
                // Without a starting location, the recorded bearing is retained regardless of the north setting
                targetBearing = vector?.direction ?: bearing,
                traveledDistance = traveledDistance,
                offRouteDistance = offRouteDistance
            )
        }

        companion object {
            val defaultBearingDistance: Distance = Distance.kilometers(5f).meters()
            val maxBearingDistance: Distance = Distance.kilometers(20f).meters()
            val defaultColor: Int = AppColor.Blue.color
        }
    }

    class Beacon(
        val beacon: com.kylecorry.trail_sense.tools.beacons.domain.Beacon
    ) : Destination() {
        override fun getGuidance(input: GuidanceInput): Guidance {
            val target = beacon.coordinate
            val vector = Geography.navigate(input.location, target, input.declination, input.useTrueNorth)
            val elevationDifference = beacon.elevation?.let { it - input.elevation.meters().value }
            return Guidance(
                targetBearing = vector.direction,
                remainingDistance = Distance.meters(vector.distance),
                remainingElevationGain = elevationDifference?.takeIf { it >= 0 }?.let { Distance.meters(it) },
                remainingElevationLoss = elevationDifference?.takeIf { it <= 0 }
                    ?.let { Distance.meters(it.absoluteValue) }
            )
        }
    }
}
