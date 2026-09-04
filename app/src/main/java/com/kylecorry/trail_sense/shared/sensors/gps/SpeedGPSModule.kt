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
    ) {
        val locations = locationHistory.toList()

        val oldestLocation = locations.firstOrNull()

        // If the speed is zero, estimate the speed
        if (previousData.speed.value == 0f && oldestLocation != null) {
            val currentLocation = ApproximateCoordinate.from(
                previousData.location,
                Distance.meters(previousData.horizontalAccuracy?.real(10f) ?: 10f)
            )

            previousData.speed = SpeedEstimator.calculate(
                oldestLocation.first,
                currentLocation,
                oldestLocation.second,
                previousData.time
            )
        }

        // Add to location history every second
        if (locations.isEmpty() || Duration.between(locations.last().second, previousData.time).seconds >= 1) {
            locationHistory.add(
                ApproximateCoordinate.from(
                    previousData.location,
                    Distance.meters(previousData.horizontalAccuracy?.real(10f) ?: 10f)
                ) to previousData.time
            )
        }
    }
}
