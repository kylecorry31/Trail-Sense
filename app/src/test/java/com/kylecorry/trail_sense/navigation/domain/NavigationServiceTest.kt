package com.kylecorry.trail_sense.navigation.domain

import android.graphics.Color
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NavigationServiceTest {

    private val service = NavigationService()

    @Test
    fun nearby() {
        val mtWashington = Coordinate(44.2706, -71.3036)
        val beacons = listOf(
            Beacon(0, "Tip top house", Coordinate(44.2705, -71.3036), color = Color.BLACK, visible = true),
            Beacon(1, "Crawford", Coordinate(44.2709, -71.3056), color = Color.BLACK, visible = true),
            Beacon(2, "Pinkham", Coordinate(44.2571, -71.2530), color = Color.BLACK, visible = true)
        )

        val near5km = service.getNearbyBeacons(mtWashington, beacons, 100, 0f, 5000f).map { it.id }
        val near500m = service.getNearbyBeacons(mtWashington, beacons, 100, 0f, 500f).map { it.id }

        assertEquals(listOf(0L, 1L, 2L), near5km)
        assertEquals(listOf(0L, 1L), near500m)
    }

    @Test
    fun eta(){
        val location = Coordinate(44.2571, -71.2530)
        val speed = 1.5f
        val altitude = 1000f

        val destination = Coordinate(44.2706, -71.3036)
        val destinationAltitude = 1900f
        val beacon = Beacon(0, "", destination, elevation = destinationAltitude, color = Color.BLACK)

        val linearEta = service.eta(Position(location, altitude, Bearing(0f), speed), beacon)

        val linearEtaDownhill = service.eta(Position(location, destinationAltitude, Bearing(0f), speed), beacon.copy(elevation = altitude))

        assertEquals(127L, linearEta.toMinutes())
        assertEquals(47L, linearEtaDownhill.toMinutes())
    }
}