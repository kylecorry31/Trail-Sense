package com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

internal class NavigationBearingEntityTest {

    @Test
    fun `a bearing with a start survives a round trip`() {
        val bearing = NavigationBearing(
            4,
            123.5f,
            Coordinate(10.5, -20.25),
            Instant.ofEpochMilli(1_700_000_000_000),
            true
        )

        assertEquals(bearing, NavigationBearingEntity.from(bearing).toNavigationBearing())
    }

    @Test
    fun `a bearing without a start survives a round trip`() {
        val bearing = NavigationBearing(0, 45f, null, null, false)

        assertEquals(bearing, NavigationBearingEntity.from(bearing).toNavigationBearing())
    }

    @Test
    fun `the start location is unknown if either coordinate is missing`() {
        assertEquals(null, NavigationBearingEntity(0f, 1.0, null, null, true).startLocation)
        assertEquals(null, NavigationBearingEntity(0f, null, 1.0, null, true).startLocation)
    }
}
