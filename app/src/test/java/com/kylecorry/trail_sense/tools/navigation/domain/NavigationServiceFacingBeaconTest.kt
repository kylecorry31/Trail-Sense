package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class NavigationServiceFacingBeaconTest {

    private val service = NavigationService()
    private val origin = Coordinate.zero

    // Due east is 90 degrees, due north is 0 degrees, and slightly west of north is just under 360
    private val east = beacon(1, Coordinate(0.0, 0.01))
    private val north = beacon(2, Coordinate(0.01, 0.0))
    private val northNorthWest = beacon(3, Coordinate(0.01, -0.0018))

    private fun beacon(id: Long, coordinate: Coordinate) = Beacon(id, "", coordinate)

    @ParameterizedTest
    @CsvSource("71, 1", "109, 1", "69, ", "111, ")
    fun `a beacon is faced within 20 degrees`(azimuth: Float, expectedId: Long?) {
        val facing = service.getFacingBeacon(origin, azimuth, listOf(east), 0f)

        assertEquals(expectedId, facing?.id)
    }

    @Test
    fun `facing across north finds a beacon on the other side of north`() {
        assertEquals(3L, service.getFacingBeacon(origin, 5f, listOf(northNorthWest), 0f)?.id)
        assertEquals(2L, service.getFacingBeacon(origin, 355f, listOf(north), 0f)?.id)
    }

    @Test
    fun `the beacon closest to the facing direction is chosen`() {
        val facing = service.getFacingBeacon(origin, 2f, listOf(northNorthWest, north), 0f)

        assertEquals(2L, facing?.id)
    }

    @Test
    fun `no beacons are faced when there are no beacons`() {
        assertNull(service.getFacingBeacon(origin, 0f, emptyList(), 0f))
    }

    @Test
    fun `the north reference changes which beacon is faced`() {
        // A declination of 10 makes the beacon due east read as 80 degrees from magnetic north
        assertEquals(1L, service.getFacingBeacon(origin, 80f, listOf(east), 10f, false)?.id)
        assertNull(service.getFacingBeacon(origin, 80f, listOf(north), 10f, false))
        assertNull(service.getFacingBeacon(origin, 100f, listOf(east), 10f, false))
    }
}
