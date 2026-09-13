package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import com.kylecorry.trail_sense.shared.sensors.gps.durationSince
import com.kylecorry.trail_sense.shared.sensors.speedometer.SpeedEstimator

class SpeedGPSModule : GPSModule {
    // Location and fix elapsed realtime nanos
    private val locationHistory = RingBuffer<Pair<ApproximateCoordinate, Long>>(10)

    override suspend fun update(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ): Boolean {
        val locations = locationHistory.toList()

        val currentLocation = ApproximateCoordinate.from(
            newData.location,
            Distance.meters(newData.horizontalAccuracy?.real(10f) ?: 10f)
        )

        val oldestLocation = locations.firstOrNull()

        val currentSpeedAccuracy = newData.speedAccuracy
        val shouldReplaceSpeed = currentSpeedAccuracy != null && newData.speed.value < currentSpeedAccuracy * 0.68

        // If the speed is zero, estimate the speed
        if (shouldReplaceSpeed && oldestLocation != null) {
            newData.speed = SpeedEstimator.calculate(
                oldestLocation.first,
                currentLocation,
                newData.durationSince(oldestLocation.second)
            )
            newData.speedSource = SpeedSource.PositionDerived
            newData.speedAccuracy = null
        }

        // Add to location history every second
        if (locations.isEmpty() || newData.durationSince(locations.last().second).seconds >= 1) {
            locationHistory.add(currentLocation to newData.eventTimeElapsedNanos)
        }

        return true
    }
}
