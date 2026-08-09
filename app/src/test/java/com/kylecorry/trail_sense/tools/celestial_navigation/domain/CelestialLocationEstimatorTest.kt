package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CelestialLocationEstimatorTest {

    @Test
    fun `repeated fixes improve accuracy`() {
        val estimator = CelestialLocationEstimator()
        val location = Coordinate(41.7, -71.5)

        val first = estimator.add(location, 10_000f).estimate
        repeat(3) { estimator.add(location, 10_000f) }
        val fourth = estimator.add(location, 10_000f).estimate

        assertEquals(10_000f, first.accuracyMeters, 0.1f)
        assertTrue(fourth.accuracyMeters < first.accuracyMeters)
        assertEquals(5, fourth.sampleCount)
    }

    @Test
    fun `nearby fixes are combined`() {
        val estimator = CelestialLocationEstimator()

        estimator.add(Coordinate(41.7000, -71.5000), 5000f)
        val estimate = estimator.add(Coordinate(41.7002, -71.4998), 5000f).estimate

        assertEquals(41.7001, estimate.location.latitude, 0.0001)
        assertEquals(-71.4999, estimate.location.longitude, 0.0001)
    }

    @Test
    fun `longitude averaging crosses the date line`() {
        val estimator = CelestialLocationEstimator()

        estimator.add(Coordinate(0.0, 179.9), 5000f)
        val estimate = estimator.add(Coordinate(0.0, -179.9), 5000f).estimate

        assertTrue(kotlin.math.abs(estimate.location.longitude) > 179.9)
    }

    @Test
    fun `distant outlier is rejected after lock`() {
        val estimator = CelestialLocationEstimator()
        repeat(3) {
            estimator.add(Coordinate(41.7, -71.5), 5000f)
        }

        val update = estimator.add(Coordinate(0.0, 0.0), 5000f)

        assertFalse(update.accepted)
        assertEquals(3, update.estimate.sampleCount)
        assertTrue(update.estimate.location.distanceTo(Coordinate(41.7, -71.5)) < 1f)
    }
}
