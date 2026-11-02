package com.kylecorry.trail_sense.tools.navigation.domain.astronomy

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal class NavAstronomyServiceTest {

    private val location = Coordinate(0.0, 0.0)

    private fun astronomyAt(time: String) =
        AstronomyService(Clock.fixed(Instant.parse(time), ZoneOffset.UTC))

    @Test
    fun `sun is up at noon and down at midnight`() {
        assertTrue(NavAstronomyService(astronomyAt("2024-03-20T12:00:00Z")).getData(location).isSunUp)
        assertFalse(NavAstronomyService(astronomyAt("2024-03-20T00:00:00Z")).getData(location).isSunUp)
    }

    @Test
    fun `bearings come from the astronomy positions`() {
        val astronomy = astronomyAt("2024-03-20T12:00:00Z")

        val data = NavAstronomyService(astronomy).getData(location)

        assertEquals(astronomy.getSunPosition(location).azimuth.value, data.sunBearing, 0.001f)
        assertEquals(astronomy.getMoonPosition(location).azimuth.value, data.moonBearing, 0.001f)
    }
}
