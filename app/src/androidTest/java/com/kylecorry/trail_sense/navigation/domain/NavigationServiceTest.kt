package com.kylecorry.trail_sense.navigation.domain

import android.graphics.Color
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.Position
import org.junit.Assert
import org.junit.Test

internal class NavigationServiceTest {

    private val service = NavigationService()

    @Test
    fun nearby() {
        val mtWashington = Coordinate(44.2706, -71.3036)
        val beacons = listOf(
            Beacon(0, "Tip top house", Coordinate(44.2705, -71.3036), color = Color.BLACK),
            Beacon(1, "Crawford", Coordinate(44.2709, -71.3056), color = Color.BLACK),
            Beacon(2, "Pinkham", Coordinate(44.2571, -71.2530), color = Color.BLACK)
        )

        val near5km = service.getNearbyBeacons(mtWashington, beacons, 100, 5000f).map { it.id }
        val near500m = service.getNearbyBeacons(mtWashington, beacons, 100, 500f).map { it.id }

        Assert.assertEquals(listOf(0L, 1L, 2L), near5km)
        Assert.assertEquals(listOf(0L, 1L), near500m)
    }

    @Test
    fun eta(){
        val location = Coordinate(44.2571, -71.2530)
        val speed = 1.5f
        val altitude = 1000f

        val destination = Coordinate(44.2706, -71.3036)
        val destinationAltitude = 1900f
        val beacon = Beacon(0, "", destination, elevation = destinationAltitude, color = Color.BLACK)

        val linearEta = service.eta(Position(location, altitude, Bearing(0f), speed), beacon, false)
        val nonLinearEta = service.eta(Position(location, altitude, Bearing(0f), speed), beacon, true)

        val linearEtaDownhill = service.eta(Position(location, destinationAltitude, Bearing(0f), speed), beacon.copy(elevation = altitude), false)
        val nonLinearEtaDownhill = service.eta(Position(location, destinationAltitude, Bearing(0f), speed), beacon.copy(elevation = altitude), true)

        Assert.assertEquals(137L, linearEta.toMinutes())
        Assert.assertEquals(165L, nonLinearEta.toMinutes())
        Assert.assertEquals(47L, linearEtaDownhill.toMinutes())
        Assert.assertEquals(75L, nonLinearEtaDownhill.toMinutes())
    }
}