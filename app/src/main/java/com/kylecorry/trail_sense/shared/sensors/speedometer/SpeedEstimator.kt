package com.kylecorry.trail_sense.shared.sensors.speedometer

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import java.time.Duration
import java.time.Instant

object SpeedEstimator {

    fun calculate(
        lastLocation: ApproximateCoordinate,
        newLocation: ApproximateCoordinate,
        lastTime: Instant,
        newTime: Instant
    ): Speed {
        // If the location is unset, the speed is zero
        if (lastLocation.coordinate == Coordinate.zero || newLocation.coordinate == Coordinate.zero) {
            return Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
        }

        val distance = lastLocation.coordinate.distanceTo(newLocation.coordinate)
        val time = Duration.between(lastTime, newTime)

        // If the time is zero, the speed is zero
        if (time.isZero || time.isNegative) {
            return Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
        }

        val speed = distance / (time.toMillis() / 1000f)

        // Estimate the range of the speed, given that the locations are as far apart as the accuracy
        val maxDistance = distance + lastLocation.accuracy.distance + newLocation.accuracy.distance
        val minDistance =
            (distance - lastLocation.accuracy.distance - newLocation.accuracy.distance).coerceAtLeast(
                0f
            )
        val maxSpeed = maxDistance / (time.toMillis() / 1000f)
        val minSpeed = minDistance / (time.toMillis() / 1000f)
        val speedAccuracy = (maxSpeed - minSpeed) / 2f

        // Use the same logic as the GPS speedometer to determine if the speed is valid
        val trueSpeed = if (speed < speedAccuracy * 0.68) {
            0f
        } else {
            speed
        }

        return Speed(trueSpeed, DistanceUnits.Meters, TimeUnits.Seconds)
    }

}