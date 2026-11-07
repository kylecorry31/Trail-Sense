package com.kylecorry.trail_sense.shared.declination

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class DeclinationUtilsTest {

    @Test
    fun `true north bearing is unchanged when using true north`() {
        assertEquals(90f, DeclinationUtils.fromTrueNorthBearing(90f, 10f, true), 0.01f)
    }

    @Test
    fun `true north bearing is converted to magnetic north when not using true north`() {
        assertEquals(80f, DeclinationUtils.fromTrueNorthBearing(90f, 10f, false), 0.01f)
    }

    @ParameterizedTest
    @CsvSource(
        "90, 10, true, true, 90",
        "90, 10, false, false, 90",
        "90, 10, false, true, 100",
        "90, 10, true, false, 80",
        "355, 10, false, true, 5",
        "5, 10, true, false, 355",
        "90, -10, false, true, 80"
    )
    fun `converts bearings between north references`(
        bearing: Float,
        declination: Float,
        isTrueNorth: Boolean,
        toTrueNorth: Boolean,
        expected: Float
    ) {
        assertEquals(
            expected,
            DeclinationUtils.convertBearing(bearing, declination, isTrueNorth, toTrueNorth),
            0.01f
        )
    }
}
