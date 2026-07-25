package com.kylecorry.trail_sense.tools.climate.infrastructure

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class MonthlyValueInterpolatorTest {

    @Test
    fun canInterpolateFromDecemberTowardJanuary() {
        val monthlyValues = Month.entries.associateWith { it.value.toFloat() }
        val interpolator = MonthlyValueInterpolator()

        val decemberValue = interpolator.interpolate(LocalDate.of(2025, Month.DECEMBER, 15), monthlyValues)
        val lateDecemberValue = interpolator.interpolate(LocalDate.of(2025, Month.DECEMBER, 16), monthlyValues)

        assertTrue(
            lateDecemberValue < decemberValue,
            "December 16 should move from December's value toward January's lower value"
        )
    }
}
