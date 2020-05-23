package com.kylecorry.trail_sense.shared.math

import org.junit.Test

import org.junit.Assert.*
import org.threeten.bp.LocalDateTime

class TimeMathTest {

    @Test
    fun getPercentOfDuration() {
        val start = LocalDateTime.of(2020, 1, 1, 0, 0)
        val end = LocalDateTime.of(2020, 1, 2, 0, 0)
        val current = LocalDateTime.of(2020, 1, 1, 12, 0)

        val percent = getPercentOfDuration(start, end, current)

        assertEquals(0.5f, percent, 0.001f)

    }
}