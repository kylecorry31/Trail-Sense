package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.Coordinate
import org.junit.Test

import org.junit.Assert.*

class NavigationServiceTest {

    @Test
    fun navigate() {
        val start = Coordinate(0.0, 1.0)
        val end = Coordinate(10.0, -8.0)
        val service = NavigationService()

        val vector = service.getNavigationVector(start, end)

        val expected = FloatArray(3)
        Location.distanceBetween(0.0, 1.0, 10.0, -8.0, expected)

        assertEquals(Bearing(expected[1]).value, vector.direction.value, 0.005f)
        assertEquals(expected[0], vector.distance, 0.005f)
    }

    @Test
    fun navigatesPath(){
        val path = Path("test", listOf(
            Coordinate(1.0, 0.0),
            Coordinate(1.0, 1.0),
            Coordinate(2.0, 1.0)
        ))

        assertNextDestination(Coordinate(1.0, 0.0), Coordinate(0.0, 0.0), path)
        assertNextDestination(Coordinate(1.0, 1.0), Coordinate(1.0, 0.0), path)
        assertNextDestination(Coordinate(2.0, 1.0), Coordinate(1.0, 1.0), path)
        assertNextDestination(Coordinate(2.0, 1.0), Coordinate(2.0, 1.0), path)
        assertNextDestination(Coordinate(2.0, 1.0), Coordinate(3.0, 2.0), path)
        assertNextDestination(Coordinate(1.0, 1.0), Coordinate(1.0, 0.5), path)
        assertNextDestination(Coordinate(1.0, 1.0), Coordinate(0.8, 0.9), path)
        assertNextDestination(Coordinate(1.0, 1.0), Coordinate(0.0, 1.0), path)
    }

    private fun assertNextDestination(expected: Coordinate?, from: Coordinate, path: Path){
        val service = NavigationService()
        val actual = service.getNextDestination(from, path)
        assertEquals(expected, actual)
    }
}