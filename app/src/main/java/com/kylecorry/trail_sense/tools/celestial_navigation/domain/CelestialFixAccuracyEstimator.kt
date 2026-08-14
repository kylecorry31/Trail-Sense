package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.units.Coordinate
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Estimates the accuracy of a location calculated from star readings by looking at how well the
 * stars line up with where they should be at that location.
 */
class CelestialFixAccuracyEstimator(
    private val minimumAccuracyMeters: Float = 5000f,
    private val maximumAccuracyMeters: Float = 500_000f
) {

    /**
     * @param readings the star readings used to calculate the location
     * @param location the calculated location
     * @param confidence the confidence of the star matches [0, 1]
     * @return the estimated accuracy in meters
     */
    fun getAccuracy(
        readings: List<StarReading>,
        location: Coordinate,
        confidence: Float
    ): Float {
        val confidencePenalty = (1 - confidence.coerceIn(0f, 1f)) * CONFIDENCE_PENALTY_METERS
        val residuals = getResiduals(readings, location)

        // Each residual has an altitude and azimuth component, and the location solver removes a
        // constant altitude and azimuth bias, so only the scatter around the bias is an error
        val degreesOfFreedom = 2 * residuals.size - FITTED_PARAMETERS
        if (degreesOfFreedom <= 0) {
            return maximumAccuracyMeters
        }

        val altitudeBias = residuals.map { it.altitudeError }.average().toFloat()
        val azimuthBias = residuals.map { it.azimuthError }.average().toFloat()
        val sumOfSquares = residuals.sumOf {
            val altitudeError = (it.altitudeError - altitudeBias).toDouble()
            // An azimuth offset is a rotation about the zenith, so it covers less of the sky the
            // higher the star is
            val azimuthError =
                (it.azimuthError - azimuthBias).toDouble() * cos(it.altitude.toRadians())
            altitudeError * altitudeError + azimuthError * azimuthError
        }
        val rmsDegrees = sqrt(sumOfSquares / degreesOfFreedom).toFloat()

        return max(rmsDegrees * METERS_PER_DEGREE, confidencePenalty)
            .coerceIn(minimumAccuracyMeters, maximumAccuracyMeters)
    }

    /**
     * The altitude and azimuth error of each reading in degrees
     */
    private fun getResiduals(
        readings: List<StarReading>,
        location: Coordinate
    ): List<Residual> {
        return readings.map { reading ->
            val expected = Astronomy.getStarPosition(reading.star, reading.time, location, true)
            val azimuth = reading.azimuth
            Residual(
                reading.altitude,
                reading.altitude - expected.altitude,
                if (azimuth == null) 0f else deltaAngle(expected.azimuth.value, azimuth)
            )
        }
    }

    private data class Residual(
        val altitude: Float,
        val altitudeError: Float,
        val azimuthError: Float
    )

    companion object {
        private const val METERS_PER_DEGREE = 111_320f
        private const val CONFIDENCE_PENALTY_METERS = 100_000f

        // Latitude, longitude, altitude bias and azimuth bias
        private const val FITTED_PARAMETERS = 4
    }
}
