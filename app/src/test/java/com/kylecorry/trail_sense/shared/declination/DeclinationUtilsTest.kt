package com.kylecorry.trail_sense.shared.declination

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DeclinationUtilsTest {

    @Test
    fun `true north bearing is unchanged when using true north`() {
        assertEquals(90f, DeclinationUtils.fromTrueNorthBearing(90f, 10f, true), 0.01f)
    }

    @Test
    fun `true north bearing is converted to magnetic north when not using true north`() {
        assertEquals(80f, DeclinationUtils.fromTrueNorthBearing(90f, 10f, false), 0.01f)
    }
}
