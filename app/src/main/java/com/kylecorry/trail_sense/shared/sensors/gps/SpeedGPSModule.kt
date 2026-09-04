package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.sensors.speedometer.SpeedEstimator
import java.time.Duration
import java.time.Instant

class SpeedGPSModule : GPSModule {
    private val locationHistory = RingBuffer<Pair<ApproximateCoordinate, Instant>>(10)

    override fun update(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ): Boolean {
        val locations = locationHistory.toList()

        val currentLocation = ApproximateCoordinate.from(
            newData.location,
            Distance.meters(newData.horizontalAccuracy?.real(10f) ?: 10f)
        )

        val oldestLocation = locations.firstOrNull()

        // If the speed is zero, estimate the speed
        if (newData.speed.value == 0f && oldestLocation != null) {
            newData.speed = SpeedEstimator.calculate(
                oldestLocation.first,
                currentLocation,
                oldestLocation.second,
                newData.time
            )
        }

        // Add to location history every second
        if (locations.isEmpty() || Duration.between(locations.last().second, newData.time).seconds >= 1) {
            locationHistory.add(currentLocation to newData.time)
        }

        return true
    }
}
