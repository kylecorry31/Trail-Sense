package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

internal class NavigationBearingCalculatorTest {

    private val origin = Coordinate(0.0, 0.0)

    @Test
    fun getDeclinationWithAutoDeclinationReturnsMagneticModelDeclination() {
        // Value from Sol's WMM2025 model for New York on 2026-06-01
        val declination = NavigationBearingCalculator.getDeclination(
            Coordinate(40.7128, -74.0060),
            elevation = 0f,
            time = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli(),
            useAutoDeclination = true,
            declinationOverride = 5f
        )
        assertEquals(-12.477813f, declination, 0.001f)
    }

    @Test
    fun getDeclinationWithoutAutoDeclinationReturnsOverride() {
        val declination = NavigationBearingCalculator.getDeclination(
            origin,
            elevation = 0f,
            time = 0L,
            useAutoDeclination = false,
            declinationOverride = 5f
        )
        assertEquals(5f, declination, 0.0f)
    }
}
