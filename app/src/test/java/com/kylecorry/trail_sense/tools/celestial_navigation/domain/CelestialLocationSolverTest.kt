package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CelestialLocationSolverTest {

    private val solver = CelestialLocationSolver()
    private val location = Coordinate(41.7, -71.5)
    private val time = ZonedDateTime.of(2026, 8, 13, 22, 0, 0, 0, ZoneId.of("America/New_York"))

    @Test
    fun `solves the location from perfect readings`() {
        val solved = solver.solve(getReadings(), Coordinate(39.0, -77.0))

        val error = solved?.distanceTo(location)
        assertTrue(error != null && error < 1000f, "Expected a close fix, but was $error m away")
    }

    @Test
    fun `solves the location with a compass and inclination bias`() {
        val readings = getReadings().map {
            StarReading(it.star, it.altitude + 2f, it.azimuth?.plus(15f), it.time)
        }

        val solved = solver.solve(readings, Coordinate(39.0, -77.0))

        val error = solved?.distanceTo(location)
        assertTrue(error != null && error < 25_000f, "Expected a close fix, but was $error m away")
    }

    @Test
    fun `solves quickly`() {
        val readings = getReadings()
        // Warm up
        solver.solve(readings, Coordinate(39.0, -77.0))

        val start = System.currentTimeMillis()
        repeat(10) {
            solver.solve(readings, Coordinate(39.0, -77.0))
        }
        val elapsed = (System.currentTimeMillis() - start) / 10f

        assertTrue(elapsed < 50f, "Expected a fast solve, but took $elapsed ms")
    }

    @Test
    fun `too few readings`() {
        assertNull(solver.solve(getReadings().take(2), Coordinate(39.0, -77.0)))
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
