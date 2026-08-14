package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class CelestialFixAccuracyEstimatorTest {

    private val estimator = CelestialFixAccuracyEstimator()
    private val location = Coordinate(41.7, -71.5)
    private val time = ZonedDateTime.of(2026, 8, 13, 22, 0, 0, 0, ZoneId.of("America/New_York"))

    @Test
    fun `perfect readings are as accurate as possible`() {
        val accuracy = estimator.getAccuracy(getReadings(), location, 1f)
        assertEquals(5000f, accuracy, 1f)
    }

    @Test
    fun `a constant altitude and azimuth bias is ignored`() {
        val readings = getReadings().map {
            StarReading(it.star, it.altitude + 3f, it.azimuth?.plus(5f), it.time)
        }

        val accuracy = estimator.getAccuracy(readings, location, 1f)

        assertEquals(5000f, accuracy, 1f)
    }

    @Test
    fun `scattered readings are less accurate`() {
        val random = Random(1)
        val readings = getReadings().map {
            StarReading(
                it.star,
                it.altitude + random.nextDouble(-1.0, 1.0).toFloat(),
                it.azimuth?.plus(random.nextDouble(-1.0, 1.0).toFloat()),
                it.time
            )
        }

        val accuracy = estimator.getAccuracy(readings, location, 1f)

        assertTrue(accuracy > 20_000f, "Expected a low accuracy, but was $accuracy")
        assertTrue(accuracy < 200_000f, "Expected a usable accuracy, but was $accuracy")
    }

    @Test
    fun `unsure star matches are less accurate`() {
        val accuracy = estimator.getAccuracy(getReadings(), location, 0.5f)
        assertEquals(50_000f, accuracy, 1f)
    }

    @Test
    fun `too few readings to estimate the error`() {
        val accuracy = estimator.getAccuracy(getReadings().take(2), location, 1f)
        assertEquals(500_000f, accuracy, 1f)
    }

    private fun getReadings(): List<StarReading> {
        return STAR_CATALOG
            .filter { it.magnitude <= 2f }
            .map { it to Astronomy.getStarPosition(it, time, location, true) }
            .filter { it.second.altitude > 20f }
            .take(6)
            .map { StarReading(it.first, it.second.altitude, it.second.azimuth.value, time) }
    }
}
