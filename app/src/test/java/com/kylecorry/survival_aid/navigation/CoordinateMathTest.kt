package com.kylecorry.survival_aid.navigation

import org.junit.Test

import org.junit.Assert.*

class CoordinateMathTest {

    @Test
    fun getBearing() {
        val start = Coordinate(39.099912f, -94.581213f)
        val end = Coordinate(38.627089f, -90.200203f)
        assertEquals(96.51f, CoordinateMath.getBearing(start, end), 0.01f)
    }

    @Test
    fun getDistance() {
        val start = Coordinate(36.12f, -86.67f)
        val end = Coordinate(33.94f, -118.4f)
        assertEquals(2887.259f, CoordinateMath.getDistance(start, end), 0.001f)
    }
}