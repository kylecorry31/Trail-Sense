package com.kylecorry.trail_sense.navigation.paths.domain.point_finder

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class NearestPathPointNavigatorTest {

    @Test
    fun getNextPoint() = runBlocking {
        val path = listOf(
            PathPoint(1, 1, Coordinate(1.0, 2.0)),
            PathPoint(1, 1, Coordinate(2.0, 3.0)),
            PathPoint(1, 1, Coordinate(3.0, 4.0)),
            PathPoint(1, 1, Coordinate(4.0, 5.0)),
        )


        val navigator = NearestPathPointNavigator()

        assertEquals(path[0], navigator.getNextPoint(path, Coordinate(0.0, 0.0)))
        assertEquals(path[1], navigator.getNextPoint(path, Coordinate(2.0, 2.8)))
        assertEquals(path[3], navigator.getNextPoint(path, Coordinate(10.0, 10.0)))
        assertNull(navigator.getNextPoint(emptyList(), Coordinate(10.0, 10.0)))
    }
}