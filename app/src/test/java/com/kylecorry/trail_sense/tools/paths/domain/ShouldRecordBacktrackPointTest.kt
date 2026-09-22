package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShouldRecordBacktrackPointTest {

    private val origin = Coordinate(0.0, 0.0)

    @Test
    fun recordsWhenThereIsNoPreviousPoint() {
        val specification = ShouldRecordBacktrackPoint(Distance.meters(10f))

        assertTrue(specification.isSatisfiedBy(null, point(origin)))
    }

    @Test
    fun recordsWhenTheMinimumIsDisabledOrZero() {
        assertTrue(ShouldRecordBacktrackPoint(null).isSatisfiedBy(point(origin), point(origin)))
        assertTrue(
            ShouldRecordBacktrackPoint(Distance.meters(0f))
                .isSatisfiedBy(point(origin), point(origin))
        )
    }

    @Test
    fun skipsPointsCloserThanTheMinimum() {
        val specification = ShouldRecordBacktrackPoint(Distance.meters(10f))
        val nearby = origin.plus(9.9, Bearing.from(0f))

        assertFalse(specification.isSatisfiedBy(point(origin), point(nearby)))
    }

    @Test
    fun recordsPointsBeyondTheMinimum() {
        val specification = ShouldRecordBacktrackPoint(Distance.meters(10f))
        val farEnough = origin.plus(11.0, Bearing.from(0f))

        assertTrue(specification.isSatisfiedBy(point(origin), point(farEnough)))
    }

    private fun point(coordinate: Coordinate): PathPoint {
        return PathPoint(0, 0, coordinate)
    }
}
