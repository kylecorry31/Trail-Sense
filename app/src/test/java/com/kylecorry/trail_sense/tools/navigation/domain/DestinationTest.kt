package com.kylecorry.trail_sense.tools.navigation.domain

import android.graphics.Color
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.GuidanceInput
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import com.kylecorry.trail_sense.tools.paths.domain.Path as PathModel

internal class DestinationTest {

    private val origin = Coordinate(0.0, 0.0)

    private fun input(
        location: Coordinate = origin,
        elevation: Float = 0f,
        declination: Float = 0f,
        useTrueNorth: Boolean = true
    ) = GuidanceInput(location, Distance.meters(elevation), declination, useTrueNorth)

    private fun Distance?.metersOrNull() = this?.meters()?.value

    @Test
    fun `beacon guidance points to and measures the distance to the beacon`() {
        val beacon = Beacon(1, "", Coordinate(0.0, 0.01), color = Color.BLACK)
        val destination = Destination.Beacon(beacon)

        val guidance = destination.getGuidance(input())

        assertEquals(90f, guidance.targetBearing.value, 0.1f)
        assertEquals(origin.distanceTo(beacon.coordinate), guidance.remainingDistance.metersOrNull()!!, 0.5f)
        assertNull(guidance.traveledDistance)
        assertNull(guidance.offRouteDistance)
        assertNull(guidance.progress)
        assertFalse(guidance.arrived)
    }

    @ParameterizedTest
    @MethodSource("provideNorthReferences")
    fun `beacon guidance bearing uses the north reference`(useTrueNorth: Boolean, expectedBearing: Float) {
        val destination = Destination.Beacon(Beacon(1, "", Coordinate(0.0, 0.01), color = Color.BLACK))

        val guidance = destination.getGuidance(input(declination = 10f, useTrueNorth = useTrueNorth))

        assertEquals(expectedBearing, guidance.targetBearing.value, 0.1f)
    }

    @ParameterizedTest(name = "beacon at {0} from {1}")
    @MethodSource("provideBeaconElevations")
    fun `beacon guidance reports remaining elevation gain and loss`(
        beaconElevation: Float?,
        currentElevation: Float,
        expectedGain: Float?,
        expectedLoss: Float?
    ) {
        val destination = Destination.Beacon(
            Beacon(1, "", Coordinate(0.0, 0.01), elevation = beaconElevation, color = Color.BLACK)
        )

        val guidance = destination.getGuidance(input(elevation = currentElevation))

        assertEquals(expectedGain, guidance.remainingElevationGain.metersOrNull())
        assertEquals(expectedLoss, guidance.remainingElevationLoss.metersOrNull())
    }

    @Test
    fun `beacon elevation is compared in meters regardless of the input unit`() {
        val destination = Destination.Beacon(
            Beacon(1, "", Coordinate(0.0, 0.01), elevation = 100f, color = Color.BLACK)
        )
        val input = GuidanceInput(origin, Distance.feet(100f), 0f, true)

        val guidance = destination.getGuidance(input)

        assertEquals(100f - 30.48f, guidance.remainingElevationGain.metersOrNull()!!, 0.01f)
    }

    @Test
    fun `bearing without a starting location only has a bearing`() {
        val destination = Destination.Bearing(Bearing.from(45f), isTrueNorth = true, declination = 10f)

        val guidance = destination.getGuidance(input(location = Coordinate(10.0, 10.0)))

        assertEquals(45f, guidance.targetBearing.value, 0.01f)
        assertNull(guidance.remainingDistance)
        assertNull(guidance.traveledDistance)
        assertNull(guidance.offRouteDistance)
        assertFalse(guidance.arrived)
    }

    @ParameterizedTest
    @CsvSource("true, true", "true, false", "false, true", "false, false")
    fun `bearing without a starting location retains the recorded bearing`(
        isTrueNorth: Boolean,
        useTrueNorth: Boolean
    ) {
        val destination = Destination.Bearing(Bearing.from(45f), isTrueNorth = isTrueNorth, declination = 10f)

        val guidance = destination.getGuidance(input(useTrueNorth = useTrueNorth))

        assertEquals(45f, guidance.targetBearing.value, 0.01f)
    }

    @Test
    fun `bearing with a starting location measures progress toward the target`() {
        val destination = Destination.Bearing(
            Bearing.from(90f),
            isTrueNorth = true,
            declination = 0f,
            startingLocation = origin,
            bearingDistance = Distance.meters(1000f)
        )
        val location = Coordinate(0.0, 0.0045)

        val guidance = destination.getGuidance(input(location = location))

        assertEquals(90f, guidance.targetBearing.value, 0.1f)
        assertNull(guidance.remainingDistance)
        assertEquals(origin.distanceTo(location), guidance.traveledDistance.metersOrNull()!!, 0.5f)
        assertEquals(0f, guidance.offRouteDistance.metersOrNull()!!, 1f)
    }

    @ParameterizedTest
    @CsvSource("0.001", "-0.001")
    fun `bearing with a starting location measures the distance off the line to the target`(latitude: Double) {
        val destination = Destination.Bearing(
            Bearing.from(90f),
            isTrueNorth = true,
            declination = 0f,
            startingLocation = origin,
            bearingDistance = Distance.meters(1000f)
        )
        val location = Coordinate(latitude, 0.0045)

        val guidance = destination.getGuidance(input(location = location))

        assertEquals(location.distanceTo(Coordinate(0.0, 0.0045)), guidance.offRouteDistance.metersOrNull()!!, 2f)
    }

    @Test
    fun `bearing at the starting location has not traveled`() {
        val destination = Destination.Bearing(
            Bearing.from(0f),
            isTrueNorth = true,
            declination = 0f,
            startingLocation = origin,
            bearingDistance = Distance.meters(1000f)
        )

        val guidance = destination.getGuidance(input())

        assertEquals(0f, guidance.traveledDistance.metersOrNull()!!, 0.01f)
        assertNull(guidance.remainingDistance)
    }

    @Test
    fun `bearing with a starting location points to the target from the current location`() {
        val destination = Destination.Bearing(
            Bearing.from(0f),
            isTrueNorth = true,
            declination = 0f,
            startingLocation = origin,
            bearingDistance = Distance.meters(1000f)
        )

        val guidance = destination.getGuidance(input(location = Coordinate(0.0, 0.001)))

        assertTrue(guidance.targetBearing.value in 340f..359f)
    }

    @Test
    fun `path guidance reports the progress along the path`() {
        val destination = pathDestination()
        val location = Coordinate(0.0, 0.0005)
        destination.navigateTo(location)

        val guidance = destination.getGuidance(input(location = location))

        val routeGuidance = destination.route.guidance
        assertEquals(90f, guidance.targetBearing.value, 0.5f)
        assertEquals(routeGuidance.remainingDistance.metersOrNull()!!, guidance.remainingDistance.metersOrNull()!!, 0.01f)
        assertEquals(routeGuidance.distanceAlong.metersOrNull()!!, guidance.traveledDistance.metersOrNull()!!, 0.01f)
        assertEquals(routeGuidance.offRouteDistance.metersOrNull()!!, guidance.offRouteDistance.metersOrNull()!!, 0.01f)
        assertEquals(routeGuidance.remainingElevationGain.metersOrNull()!!, guidance.remainingElevationGain.metersOrNull()!!, 0.01f)
        assertEquals(routeGuidance.remainingElevationLoss.metersOrNull()!!, guidance.remainingElevationLoss.metersOrNull()!!, 0.01f)
        assertEquals(routeGuidance.progress, guidance.progress!!, 0.0001f)
        assertTrue(guidance.progress!! > 0f && guidance.progress!! < 1f)
        assertFalse(guidance.arrived)
    }

    @Test
    fun `path guidance reports arrival at the end of the path`() {
        val destination = pathDestination()
        val end = Coordinate(0.0, 0.002)
        destination.navigateTo(end)

        val guidance = destination.getGuidance(input(location = end))

        assertTrue(guidance.arrived)
        assertEquals(1f, guidance.progress!!, 0.0001f)
        assertEquals(0f, guidance.remainingDistance.metersOrNull()!!, 0.01f)
    }

    @Test
    fun `path guidance bearing is relative to magnetic north when true north is not used`() {
        val destination = pathDestination()
        val location = Coordinate(0.0, 0.0005)
        destination.navigateTo(location)

        val guidance = destination.getGuidance(input(location = location, declination = 10f, useTrueNorth = false))

        assertEquals(80f, guidance.targetBearing.value, 0.5f)
    }

    @Test
    fun `path guidance reports the distance off the path`() {
        val destination = pathDestination()
        val location = Coordinate(0.0003, 0.0005)
        destination.navigateTo(location)

        val guidance = destination.getGuidance(input(location = location))

        assertEquals(origin.distanceTo(Coordinate(0.0003, 0.0)), guidance.offRouteDistance.metersOrNull()!!, 1f)
    }

    // The route only searches near the previous position, so it is placed at the location first
    private fun Destination.Path.navigateTo(location: Coordinate) {
        route.restorePosition(origin.distanceTo(location), location)
        route.navigate(location, null)
    }

    private fun pathDestination(): Destination.Path {
        val points = listOf(
            PathPoint(1, 1, Coordinate(0.0, 0.0), 0f),
            PathPoint(2, 1, Coordinate(0.0, 0.001), 100f),
            PathPoint(3, 1, Coordinate(0.0, 0.002), 50f)
        )
        val path = PathModel(
            1,
            null,
            PathStyle(LineStyle.Solid, PathPointColoringStyle.None, Color.BLACK, true),
            PathMetadata.empty
        )
        return Destination.Path(path, points)
    }

    companion object {
        @JvmStatic
        fun provideNorthReferences(): Stream<Arguments> = Stream.of(
            Arguments.of(true, 90f),
            Arguments.of(false, 80f)
        )

        // Beacon elevation, current elevation, expected gain, expected loss
        @JvmStatic
        fun provideBeaconElevations(): Stream<Arguments> = Stream.of(
            Arguments.of(150f, 100f, 50f, null),
            Arguments.of(60f, 100f, null, 40f),
            Arguments.of(100f, 100f, 0f, 0f),
            Arguments.of(null, 100f, null, null)
        )
    }
}
