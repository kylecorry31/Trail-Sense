package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.Vector
import com.kylecorry.sol.math.algebra.LinearAlgebra
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.units.Coordinate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calculates a location from star readings using the intercept method.
 *
 * Astronomy.getLocationFromStars searches for the compass and inclination bias using simulated
 * annealing, which needs hundreds of thousands of star positions - far too slow on a phone. This
 * solves for the bias directly from the residuals instead, which needs a few hundred.
 */
class CelestialLocationSolver(
    private val positionIterations: Int = 20,
    private val biasIterations: Int = 8
) {

    fun solve(readings: List<StarReading>, approximateLocation: Coordinate): Coordinate? {
        if (readings.size <= 2) {
            return null
        }

        var location = approximateLocation
        var altitudeBias = 0f
        var azimuthBias = 0f

        repeat(biasIterations) {
            location = getLocation(readings, location, altitudeBias, azimuthBias)
            if (!isValid(location)) {
                return null
            }
            // The readings share a compass and inclination bias, which looks like a constant
            // offset from where the stars actually are
            val expected = readings.map {
                Astronomy.getStarPosition(it.star, it.time, location, true)
            }
            altitudeBias += readings.zip(expected)
                .map { (reading, star) -> reading.altitude - altitudeBias - star.altitude }
                .average().toFloat()
            azimuthBias += readings.zip(expected).mapNotNull { (reading, star) ->
                val azimuth = reading.azimuth ?: return@mapNotNull null
                deltaAngle(star.azimuth.value, azimuth - azimuthBias).toDouble()
            }.average().toFloat()
        }

        return location.takeIf { isValid(it) }
    }

    /**
     * Move the location until the stars are where they should be, using lines of position.
     */
    private fun getLocation(
        readings: List<StarReading>,
        approximateLocation: Coordinate,
        altitudeBias: Float,
        azimuthBias: Float
    ): Coordinate {
        var latitude = approximateLocation.latitude.coerceIn(-90.0, 90.0)
        var longitude = approximateLocation.longitude

        repeat(positionIterations) {
            val location = Coordinate(latitude, longitude)
            // Each star gives a line of position: moving toward its azimuth raises its altitude
            val linesOfPosition = readings.map { reading ->
                val expected = Astronomy.getStarPosition(reading.star, reading.time, location, true)
                val intercept = (reading.altitude - altitudeBias) - expected.altitude
                val azimuth = ((reading.azimuth ?: expected.azimuth.value) - azimuthBias).toRadians()
                floatArrayOf(cos(azimuth), sin(azimuth)) to intercept
            }

            val correction = LinearAlgebra.robustLeastSquares(
                Matrix.create(linesOfPosition.map { it.first }.toTypedArray()),
                Vector(linesOfPosition.map { it.second }.toFloatArray()),
                minScale = 0.05f
            )

            if (!correction.norm().isFinite() || correction.norm() < 0.000001) {
                return location
            }

            // The correction is in degrees north and degrees east, so the east component has to be
            // scaled to degrees of longitude
            val newLatitude = (latitude + correction[0]).coerceIn(-89.9, 89.9)
            val east = correction[1] / cos(newLatitude.toRadians())
            latitude = newLatitude
            longitude = Coordinate.toLongitude(longitude + east)
        }

        return Coordinate(latitude, longitude)
    }

    private fun isValid(location: Coordinate): Boolean {
        return location.latitude.isFinite() && location.longitude.isFinite()
    }
}
