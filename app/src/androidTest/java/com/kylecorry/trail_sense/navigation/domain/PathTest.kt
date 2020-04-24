package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.shared.Coordinate
import org.junit.Test

import org.junit.Assert.*

class PathTest {

    @Test
    fun canReversePath() {
        val path = Path("test", listOf(
            Coordinate(0.0, 0.0),
            Coordinate(1.0, 1.0),
            Coordinate(1.5, 1.5),
            Coordinate(2.0, 2.0)
        ))

        val reversed = path.reversed()

        val expected = Path("test", listOf(
            Coordinate(2.0, 2.0),
            Coordinate(1.5, 1.5),
            Coordinate(1.0, 1.0),
            Coordinate(0.0, 0.0)
        ))
        assertEquals(expected, reversed)
    }
}