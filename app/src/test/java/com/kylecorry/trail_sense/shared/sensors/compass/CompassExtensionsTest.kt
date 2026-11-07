package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.sol.units.Bearing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class CompassExtensionsTest {

    @ParameterizedTest
    @CsvSource(
        "90, 10, true, 90",
        "90, 10, false, 100",
        "355, 10, false, 5",
        "90, -10, false, 80"
    )
    fun `gets the true north bearing`(
        bearing: Float,
        declination: Float,
        useTrueNorth: Boolean,
        expected: Float
    ) {
        val compass = mock<INorthReferencedCompass> {
            on { this.bearing } doReturn Bearing.from(bearing)
            on { this.declination } doReturn declination
            on { this.useTrueNorth } doReturn useTrueNorth
        }

        assertEquals(expected, compass.getTrueNorthBearing().value, 0.01f)
    }
}
