package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.events.FakeEventSubscriber
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSession
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSessionProgress
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class PathNavigatorTest {

    private val path = Path(1, "Path", PathStyle.default(), PathMetadata.empty)
    private val points = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0)),
        PathPoint(2, 1, Coordinate(0.0, 0.01)),
        PathPoint(3, 1, Coordinate(0.0, 0.02))
    )
    private val toEnd = PathFollow.Direction(PathDirection.ToEnd)

    private lateinit var store: PathNavigationStore
    private lateinit var paths: IPathService
    private var location = Coordinate(0.0, 0.0)
    private val events = FakeEventSubscriber()

    @BeforeEach
    fun setup(): Unit = runBlocking {
        store = PathNavigationStore(InMemoryPreferences())
        paths = mock()
        location = Coordinate(0.0, 0.0)
        whenever(paths.getPath(1)).thenReturn(path)
        whenever(paths.getWaypoints(1L)).thenReturn(points)
    }

    private fun createNavigator(): PathNavigator {
        val prefs = mock<IPathPreferences> { on { elevationSmoothing } doReturn 0f }
        return PathNavigator(prefs, store, paths, { location }, { null }, events)
    }

    private fun PathNavigator.navigateAndAwait(follow: PathFollow = toEnd) = runBlocking {
        navigate(path, follow)
        awaitRestore()
    }

    private fun PathNavigator.currentDestination() = runBlocking { destination.first() }

    @Test
    fun `there is no navigation initially`() {
        val navigator = createNavigator()

        assertNull(navigator.currentDestination())
        assertNull(navigator.currentSession())
        assertFalse(navigator.isRestoring.value)
    }

    @Test
    fun `navigating builds a route for the path from the current location`() {
        location = Coordinate(0.0, 0.005)
        val navigator = createNavigator()

        navigator.navigateAndAwait()

        val destination = navigator.currentDestination()
        assertEquals(path, destination?.path)
        assertEquals(location, navigator.currentSession()?.start)
        assertFalse(navigator.isRestoring.value)
        assertTrue(store.hasSavedRoute())
    }

    @Test
    fun `navigating to a path that no longer exists clears the navigation`() {
        whenever(runBlocking { paths.getPath(1) }).thenReturn(null)
        val navigator = createNavigator()

        navigator.navigateAndAwait()

        assertNull(navigator.currentDestination())
        assertFalse(store.hasSavedRoute())
        assertFalse(navigator.isRestoring.value)
    }

    @Test
    fun `navigating along a path that cannot be followed clears the navigation`() {
        whenever(runBlocking { paths.getWaypoints(1L) }).thenReturn(listOf(points[0], points[0].copy(id = 2)))
        val navigator = createNavigator()

        navigator.navigateAndAwait()

        assertNull(navigator.currentDestination())
        assertFalse(store.hasSavedRoute())
    }

    @Test
    fun `navigating to a point that is not on the path clears the navigation`() {
        val navigator = createNavigator()

        navigator.navigateAndAwait(PathFollow.ToPoint(99))

        assertNull(navigator.currentDestination())
        assertFalse(store.hasSavedRoute())
    }

    @Test
    fun `a failure loading the path clears the navigation`() {
        whenever(runBlocking { paths.getWaypoints(1L) }).thenThrow(IllegalStateException("db"))
        val navigator = createNavigator()

        navigator.navigateAndAwait()

        assertNull(navigator.currentDestination())
        assertFalse(store.hasSavedRoute())
        assertFalse(navigator.isRestoring.value)
    }

    @Test
    fun `a saved session is restored when created`() {
        store.save(PathNavigationSession(1, toEnd, location))

        val navigator = createNavigator()
        runBlocking { navigator.awaitRestore() }

        assertEquals(path, navigator.currentDestination()?.path)
    }

    @Test
    fun `an unreadable saved session does not crash and is cleared`() {
        store.save(PathNavigationSession(5, toEnd, location))
        whenever(runBlocking { paths.getPath(5) }).thenReturn(null)

        val navigator = createNavigator()
        runBlocking { navigator.awaitRestore() }

        assertNull(navigator.currentDestination())
        assertFalse(store.hasSavedRoute())
    }

    @Test
    fun `cancelling clears the navigation`() {
        val navigator = createNavigator()
        navigator.navigateAndAwait()

        navigator.cancel()

        assertNull(navigator.currentDestination())
        assertNull(navigator.currentSession())
        assertFalse(store.hasSavedRoute())
    }

    @Test
    fun `cancelling a path only cancels the path being navigated`() {
        val navigator = createNavigator()
        navigator.navigateAndAwait()

        assertFalse(navigator.cancel(2))
        assertNotNull(navigator.currentDestination())

        assertTrue(navigator.cancel(1))
        assertNull(navigator.currentDestination())
    }

    @Test
    fun `a cancelled session can be restored with its progress`() {
        val navigator = createNavigator()
        val session = PathNavigationSession(
            1,
            toEnd,
            location,
            PathNavigationSessionProgress(Distance.meters(500f), Coordinate(0.0, 0.0045))
        )

        navigator.restore(session)
        runBlocking { navigator.awaitRestore() }

        assertEquals(session, navigator.currentSession())
        assertNotNull(navigator.currentDestination())
    }

    @Test
    fun `progress is saved as the location changes`() {
        val navigator = createNavigator()
        navigator.navigateAndAwait()

        location = Coordinate(0.0, 0.005)
        events.publish(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)

        val progress = store.load()?.progress
        assertEquals(location, progress?.location)
        assertEquals(location.distanceTo(points[0].coordinate), progress!!.distanceAlong.meters().value, 5f)
    }

    @Test
    fun `progress is not saved again until moving far enough`() {
        val navigator = createNavigator()
        navigator.navigateAndAwait()
        val first = Coordinate(0.0, 0.005)
        location = first
        events.publish(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)

        // About 11 m east
        location = Coordinate(0.0, 0.0051)
        events.publish(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)
        assertEquals(first, store.load()?.progress?.location)

        // About 55 m east of the first
        val moved = Coordinate(0.0, 0.0055)
        location = moved
        events.publish(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)
        assertEquals(moved, store.load()?.progress?.location)
    }

    @Test
    fun `location changes without navigation save nothing`() {
        createNavigator()

        events.publish(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)

        assertFalse(store.hasSavedRoute())
    }
}
