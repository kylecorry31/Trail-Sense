package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.FakeEventSubscriber
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.FakeNavigationBearingDao
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingService
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Collections

internal class NavigatorTest {

    private class TestSettings : NavigatorSettings {
        override var useTrueNorth = true
        override var lockBearingToLocation = false
        override var bearingDistance = Distance.kilometers(5f).meters()
        override var useAutoDeclination = false
        override var declinationOverride = 0f
    }

    private val started = NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED
    private val stopped = NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED

    private val beacon = Beacon(5, "Beacon", Coordinate(0.0, 0.01))
    private val otherBeacon = Beacon(6, "Other", Coordinate(0.01, 0.0))
    private val path = Path(1, "Path", PathStyle.default(), PathMetadata.empty)
    private val pathPoints = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0)),
        PathPoint(2, 1, Coordinate(0.0, 0.01))
    )
    private val toEnd = PathFollow.Direction(PathDirection.ToEnd)
    private val location = Coordinate(0.0, 0.0)

    private lateinit var settings: TestSettings
    private lateinit var prefs: InMemoryPreferences
    private lateinit var bearings: NavigationBearingService
    private lateinit var navigator: Navigator
    private lateinit var broadcasts: MutableList<String>
    private var onLockBearingChanged: () -> Unit = {}

    @BeforeEach
    fun setup() {
        settings = TestSettings()
        prefs = InMemoryPreferences()
        bearings = NavigationBearingService(FakeNavigationBearingDao())
        broadcasts = Collections.synchronizedList(mutableListOf())

        navigator = createNavigator()
    }

    private fun createNavigator(): Navigator = runBlocking {
        val beacons = mock<IBeaconService>()
        whenever(beacons.getBeacon(5)).thenReturn(beacon)
        whenever(beacons.getBeacon(6)).thenReturn(otherBeacon)

        val paths = mock<IPathService>()
        whenever(paths.getPath(1)).thenReturn(path)
        whenever(paths.getWaypoints(1L)).thenReturn(pathPoints)
        val pathPrefs = mock<IPathPreferences> { on { elevationSmoothing } doReturn 0f }
        val pathNavigator = PathNavigator(
            pathPrefs,
            PathNavigationStore(InMemoryPreferences()),
            paths,
            { location },
            { null },
            FakeEventSubscriber()
        )

        Navigator(
            prefs,
            settings,
            beacons,
            bearings,
            pathNavigator,
            { location },
            { Distance.meters(0f) },
            object : IEventEmitter {
                override fun broadcast(eventId: String, data: EventData?) {
                    broadcasts.add(eventId)
                }
            }
        ) { onLockBearingChanged = it }
    }

    private fun destination() = runBlocking { navigator.getDestination() }

    private fun navigationEvents() = broadcasts.filter { it == started || it == stopped }

    private fun eventually(condition: () -> Boolean) = runBlocking {
        withTimeout(5000) {
            while (!condition()) delay(10)
        }
    }

    @Test
    fun `there is no destination initially`() {
        assertNull(destination())
    }

    @Test
    fun `navigating to a beacon sets the beacon as the destination`() {
        navigator.navigateTo(5)

        assertEquals(5L, (destination() as Destination.Beacon).beacon.id)
        assertEquals(5L, navigator.getBeaconDestinationId())
        assertEquals(listOf(started), navigationEvents())
    }

    @Test
    fun `the beacon destination is saved and restored by a new navigator`() {
        navigator.navigateTo(5)

        val restored = createNavigator()

        assertEquals(5L, restored.getBeaconDestinationId())
        assertEquals(5L, (runBlocking { restored.getDestination() } as Destination.Beacon).beacon.id)
    }

    @Test
    fun `a beacon destination changed externally is picked up`() {
        prefs.putLong(Navigator.BEACON_DESTINATION_ID_KEY, 6)
        prefs.onChange.publish(Navigator.BEACON_DESTINATION_ID_KEY)

        assertEquals(6L, (destination() as Destination.Beacon).beacon.id)
    }

    @Test
    fun `cancelling beacon navigation clears the destination`() {
        navigator.navigateTo(5)

        navigator.cancelBeaconNavigation()

        assertNull(destination())
        assertNull(navigator.getBeaconDestinationId())
        assertNull(prefs.getLong(Navigator.BEACON_DESTINATION_ID_KEY))
        assertEquals(listOf(started, stopped), navigationEvents())
    }

    @Test
    fun `cancelling beacon navigation when there is none does not announce a stop`() {
        navigator.cancelBeaconNavigation()

        assertEquals(emptyList<String>(), navigationEvents())
    }

    @Test
    fun `navigating to a bearing sets the bearing as the destination`() {
        runBlocking { navigator.navigateToBearing(120f) }

        val bearing = destination() as Destination.Bearing
        assertEquals(120f, bearing.bearing.value)
        assertEquals(listOf(started), navigationEvents())
    }

    @Test
    fun `navigating to a bearing replaces a beacon destination`() {
        navigator.navigateTo(5)

        runBlocking { navigator.navigateToBearing(120f) }

        assertTrue(destination() is Destination.Bearing)
        assertNull(navigator.getBeaconDestinationId())
    }

    @Test
    fun `navigating to a beacon replaces a bearing destination`() {
        runBlocking { navigator.navigateToBearing(120f) }

        navigator.navigateTo(5)

        eventually { runBlocking { bearings.getBearing().first() } == null }
        assertTrue(destination() is Destination.Beacon)
    }

    @Test
    fun `clearing the bearing clears the destination`() {
        runBlocking {
            navigator.navigateToBearing(120f)
            navigator.clearBearing()
        }

        assertNull(destination())
    }

    @Test
    fun `a bearing only has a start location when locked to its location`() {
        val start = Coordinate(1.0, 2.0)
        runBlocking { navigator.navigateToBearing(120f, start) }
        assertNull((destination() as Destination.Bearing).startingLocation)

        settings.lockBearingToLocation = true
        onLockBearingChanged()

        assertEquals(start, (destination() as Destination.Bearing).startingLocation)
    }

    @Test
    fun `a bearing uses the configured north reference and distance`() {
        settings.useTrueNorth = false
        settings.declinationOverride = 7f
        settings.bearingDistance = Distance.meters(1234f)

        runBlocking { navigator.navigateToBearing(120f) }

        val bearing = destination() as Destination.Bearing
        assertEquals(false, bearing.isTrueNorth)
        assertEquals(7f, bearing.declination)
        assertEquals(Distance.meters(1234f), bearing.bearingDistance)
    }

    @Test
    fun `navigating along a path replaces the other destinations`() {
        navigator.navigateTo(5)
        runBlocking { navigator.navigateToBearing(120f) }

        runBlocking { navigator.navigateAlongPath(path, toEnd) }

        val destination = destination() as Destination.Path
        assertEquals(path, destination.path)
        assertNull(navigator.getBeaconDestinationId())
        assertNull(runBlocking { bearings.getBearing().first() })
    }

    @Test
    fun `cancelling a path announces a stop only for the navigated path`() {
        runBlocking { navigator.navigateAlongPath(path, toEnd) }
        broadcasts.clear()

        navigator.cancelPathNavigation(99)
        assertEquals(emptyList<String>(), navigationEvents())

        navigator.cancelPathNavigation(1)
        assertEquals(listOf(stopped), navigationEvents())
        assertNull(destination())
    }

    @Test
    fun `cancelling all navigation clears every destination`() {
        navigator.navigateTo(5)
        runBlocking { navigator.cancelAllNavigation() }

        assertNull(destination())
        assertEquals(listOf(started, stopped), navigationEvents())
    }

    @Test
    fun `cancelling all navigation when not navigating does not announce a stop`() {
        runBlocking { navigator.cancelAllNavigation() }

        assertEquals(emptyList<String>(), navigationEvents())
    }

    @Test
    fun `cancelled navigation to a beacon can be restored`() {
        navigator.navigateTo(5)
        val restore = runBlocking { navigator.cancelAllNavigation() }

        runBlocking { restore() }

        assertEquals(5L, (destination() as Destination.Beacon).beacon.id)
        assertEquals(listOf(started, stopped, started), navigationEvents())
    }

    @Test
    fun `cancelled navigation to a bearing can be restored`() {
        runBlocking { navigator.navigateToBearing(120f, Coordinate(1.0, 2.0)) }
        val restore = runBlocking { navigator.cancelAllNavigation() }

        runBlocking { restore() }

        assertEquals(120f, (destination() as Destination.Bearing).bearing.value)
    }

    @Test
    fun `cancelled navigation along a path can be restored`() {
        runBlocking { navigator.navigateAlongPath(path, toEnd) }
        val restore = runBlocking { navigator.cancelAllNavigation() }

        runBlocking { restore() }

        assertEquals(path, (destination() as Destination.Path).path)
    }

    @Test
    fun `cancelled navigation is not restored over new navigation`() {
        navigator.navigateTo(5)
        val restore = runBlocking { navigator.cancelAllNavigation() }
        navigator.navigateTo(6)
        broadcasts.clear()

        runBlocking { restore() }

        assertEquals(6L, (destination() as Destination.Beacon).beacon.id)
        assertEquals(emptyList<String>(), navigationEvents())
    }

    @Test
    fun `guidance uses the true north setting unless overridden`() {
        val destination = Destination.Beacon(beacon)
        val elevation = Distance.meters(0f)

        val trueNorth = navigator.getGuidance(destination, location, elevation, 10f)
        val magnetic = navigator.getGuidance(destination, location, elevation, 10f, false)
        settings.useTrueNorth = false
        val configuredMagnetic = navigator.getGuidance(destination, location, elevation, 10f)

        assertEquals(90f, trueNorth.targetBearing.value, 0.1f)
        assertEquals(80f, magnetic.targetBearing.value, 0.1f)
        assertEquals(80f, configuredMagnetic.targetBearing.value, 0.1f)
    }

    @Test
    fun `bearing is the target bearing of the guidance`() {
        val bearing = navigator.getBearing(location, Destination.Beacon(beacon), 0f)

        assertEquals(90f, bearing.value, 0.1f)
    }
}
