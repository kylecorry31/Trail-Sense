package com.kylecorry.trail_sense.tools.navigation.domain

import android.graphics.Color
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NavigationServiceTest {

    private val service = NavigationService()

    private val mtWashington = Coordinate(44.2706, -71.3036)
    private val tipTop = beacon(0, Coordinate(44.2705, -71.3036))
    private val crawford = beacon(1, Coordinate(44.2709, -71.3056))
    private val pinkham = beacon(2, Coordinate(44.2571, -71.2530))
    private val allBeacons = listOf(tipTop, crawford, pinkham)

    private fun beacon(id: Long, coordinate: Coordinate) =
        Beacon(id, "", coordinate, color = Color.BLACK, visible = true)

    @Test
    fun nearby() {
        val beacons = listOf(
            Beacon(
                0,
                "Tip top house",
                Coordinate(44.2705, -71.3036),
                color = Color.BLACK,
                visible = true
            ),
            Beacon(
                1,
                "Crawford",
                Coordinate(44.2709, -71.3056),
                color = Color.BLACK,
                visible = true
            ),
            Beacon(2, "Pinkham", Coordinate(44.2571, -71.2530), color = Color.BLACK, visible = true)
        )

        val near5km = service.getNearbyBeacons(mtWashington, beacons, 100, 0f, 5000f).map { it.id }
        val near500m = service.getNearbyBeacons(mtWashington, beacons, 100, 0f, 500f).map { it.id }

        assertEquals(listOf(0L, 1L, 2L), near5km)
        assertEquals(listOf(0L, 1L), near500m)
    }

    @Test
    fun eta() {
        val location = Coordinate(44.2571, -71.2530)
        val speed = 1.5f
        val altitude = 1000f

        val destination = Coordinate(44.2706, -71.3036)
        val destinationAltitude = 1900f
        val beacon =
            Beacon(0, "", destination, elevation = destinationAltitude, color = Color.BLACK)

        val linearEta = service.eta(location, altitude, speed, beacon)

        val linearEtaDownhill =
            service.eta(location, destinationAltitude, speed, beacon.copy(elevation = altitude))

        assertEquals(127L, linearEta.toMinutes())
        assertEquals(47L, linearEtaDownhill.toMinutes())
    }

    @Test
    fun `eta is based on the remaining distance and elevation gain`() {
        val speed = Speed.from(1.5f, DistanceUnits.Meters, TimeUnits.Seconds)
        val flat = Guidance(Bearing.from(0f), remainingDistance = Distance.meters(1500f))
        val uphill = flat.copy(remainingElevationGain = Distance.meters(100f))

        val flatEta = service.eta(flat, speed)!!
        val uphillEta = service.eta(uphill, speed)!!

        assertEquals(1000L, flatEta.seconds)
        assertTrue(uphillEta > flatEta)
    }

    @Test
    fun `eta uses a hiking speed when standing still`() {
        val guidance = Guidance(Bearing.from(0f), remainingDistance = Distance.meters(894.08f))

        val eta = service.eta(guidance, Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds))!!

        assertEquals(1000L, eta.seconds)
    }

    @Test
    fun `eta is unknown without a remaining distance`() {
        val guidance = Guidance(Bearing.from(0f))

        assertNull(service.eta(guidance, Speed.from(1.5f, DistanceUnits.Meters, TimeUnits.Seconds)))
    }

    @Test
    fun `visible beacons are only the destination when nearby beacons are disabled`() {
        val visible = service.getVisibleBeacons(mtWashington, allBeacons, pinkham, false, 100, 5000f)

        assertEquals(listOf(2L), visible.map { it.id })
    }

    @Test
    fun `visible beacons are empty without a destination when nearby beacons are disabled`() {
        val visible = service.getVisibleBeacons(mtWashington, allBeacons, null, false, 100, 5000f)

        assertTrue(visible.isEmpty())
    }

    @Test
    fun `visible beacons include the destination even when it is not nearby`() {
        val visible = service.getVisibleBeacons(mtWashington, allBeacons, pinkham, true, 100, 500f)

        assertEquals(listOf(0L, 1L, 2L), visible.map { it.id })
    }

    @Test
    fun `visible beacons do not repeat a nearby destination`() {
        val visible = service.getVisibleBeacons(mtWashington, allBeacons, crawford, true, 100, 500f)

        assertEquals(listOf(0L, 1L), visible.map { it.id })
    }

    @Test
    fun `visible beacons skip beacons that are right on top of the user`() {
        val visible = service.getVisibleBeacons(tipTop.coordinate, listOf(tipTop, crawford), null, true, 100, 5000f)

        assertEquals(listOf(1L), visible.map { it.id })
    }

    @Test
    fun `selected beacon prefers the destination over the one being faced`() {
        val bearingToCrawford = mtWashington.bearingTo(crawford.coordinate).value

        val selected = service.getSelectedBeacon(pinkham, mtWashington, bearingToCrawford, allBeacons, 0f)

        assertEquals(2L, selected?.id)
    }

    @Test
    fun `selected beacon is the one being faced without a destination`() {
        val bearingToCrawford = mtWashington.bearingTo(crawford.coordinate).value

        val selected = service.getSelectedBeacon(null, mtWashington, bearingToCrawford, allBeacons, 0f)

        assertEquals(1L, selected?.id)
    }

    @Test
    fun `bearing is relative to the north reference`() {
        val to = Coordinate(0.0, 0.01)

        assertEquals(90f, service.getBearing(Coordinate.zero, to, 10f, true), 0.1f)
        assertEquals(80f, service.getBearing(Coordinate.zero, to, 10f, false), 0.1f)
    }

    @Test
    fun hikingSpeedIsClampedToAReasonableRange() {
        assertEquals(0.89408f, service.getHikingSpeed(0.5f), 0.00001f)
        assertEquals(1.5f, service.getHikingSpeed(1.5f), 0.00001f)
        assertEquals(1.78816f, service.getHikingSpeed(2f), 0.00001f)
        assertEquals(3f, service.getHikingSpeed(3f), 0.00001f)
    }
}
