package com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class NavigationBearingServiceTest {

    private val dao = FakeNavigationBearingDao()
    private val service = NavigationBearingService(dao)

    private fun bearing(value: Float, startTime: Instant? = Instant.now()) =
        NavigationBearing(0, value, Coordinate(1.0, 2.0), startTime, false)

    @Test
    fun `there is no bearing initially`() = runBlocking {
        assertNull(service.getBearing().first())
    }

    @Test
    fun `a set bearing is the active bearing`() = runBlocking {
        service.setBearing(bearing(90f))

        val active = service.getBearing().first()

        assertEquals(90f, active?.bearing)
        assertEquals(true, active?.isActive)
        assertEquals(Coordinate(1.0, 2.0), active?.startLocation)
    }

    @Test
    fun `setting a bearing replaces the active bearing`() = runBlocking {
        service.setBearing(bearing(90f))
        service.setBearing(bearing(180f))

        assertEquals(180f, service.getBearing().first()?.bearing)
    }

    @Test
    fun `setting null clears the active bearing`() = runBlocking {
        service.setBearing(bearing(90f))

        service.setBearing(null)

        assertNull(service.getBearing().first())
    }

    @Test
    fun `cleaning removes old inactive bearings and keeps recent and active ones`() = runBlocking {
        val now = Instant.now()
        service.setBearing(bearing(10f, now.minus(Duration.ofHours(20))))
        service.setBearing(bearing(20f, now.minus(Duration.ofHours(1))))
        service.setBearing(bearing(30f, now.minus(Duration.ofHours(20))))

        service.clean()

        assertEquals(listOf(20f, 30f), dao.all().map { it.bearing })
        assertEquals(30f, service.getBearing().first()?.bearing)
    }
}
