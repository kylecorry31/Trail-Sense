package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.units.Coordinate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class CelestialLocationEstimate(
    val location: Coordinate,
    val accuracyMeters: Float,
    val sampleCount: Int
)

data class CelestialLocationUpdate(
    val estimate: CelestialLocationEstimate,
    val accepted: Boolean
)

class CelestialLocationEstimator(
    private val maxSamples: Int = 20,
    private val minimumAccuracyMeters: Float = 2000f
) {
    private data class Sample(val location: Coordinate, val accuracyMeters: Float)

    private val samples = ArrayDeque<Sample>()

    fun add(location: Coordinate, accuracyMeters: Float): CelestialLocationUpdate {
        val normalizedAccuracy = accuracyMeters.coerceIn(5000f, 500_000f)
        val current = estimate()
        val isOutlier = current != null && samples.size >= 3 &&
                current.location.distanceTo(location) > max(
            50_000f,
            max(current.accuracyMeters * 3, normalizedAccuracy * 3)
        )

        if (!isOutlier) {
            samples.addLast(Sample(location, normalizedAccuracy))
            while (samples.size > maxSamples) {
                samples.removeFirst()
            }
        }

        return CelestialLocationUpdate(estimate() ?: error("No location samples"), !isOutlier)
    }

    fun clear() {
        samples.clear()
    }

    private fun estimate(): CelestialLocationEstimate? {
        if (samples.isEmpty()) {
            return null
        }

        val weights = samples.map { 1.0 / (it.accuracyMeters * it.accuracyMeters) }
        val weightSum = weights.sum()
        val latitude = samples.zip(weights).sumOf { (sample, weight) ->
            sample.location.latitude * weight
        } / weightSum
        val longitudeX = samples.zip(weights).sumOf { (sample, weight) ->
            cos(sample.location.longitude * PI / 180) * weight
        }
        val longitudeY = samples.zip(weights).sumOf { (sample, weight) ->
            sin(sample.location.longitude * PI / 180) * weight
        }
        val location = Coordinate(latitude, atan2(longitudeY, longitudeX) * 180 / PI)

        val combinedMeasurementAccuracy = (1 / sqrt(weightSum)).toFloat()
        val rmsScatter = sqrt(samples.map { location.distanceTo(it.location).toDouble() }
            .map { it * it }
            .average()).toFloat()
        val scatterAccuracy = 2 * rmsScatter / sqrt(samples.size.toFloat())
        val accuracy = max(
            minimumAccuracyMeters,
            max(combinedMeasurementAccuracy, scatterAccuracy)
        )

        return CelestialLocationEstimate(location, accuracy, samples.size)
    }
}
