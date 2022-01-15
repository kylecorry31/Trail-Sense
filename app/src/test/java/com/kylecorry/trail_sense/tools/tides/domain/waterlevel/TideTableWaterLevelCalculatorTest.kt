package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal class TideTableWaterLevelCalculatorTest {

    @Test
    fun calculate() {
        val table = TideTable(
            0, listOf(
                Tide.high(time(10, 1, 42), 3.25f),
                Tide.high(time(10, 14, 0), 2.71f),
                Tide.low(time(10, 19, 27), 0.39f),
                Tide.high(time(11, 14, 56), 2.54f),
                Tide.low(time(11, 20, 20), 0.4f),
            )
        )

        val calculator = TideTableWaterLevelCalculator(table)

        check(calculator, time(9, 13, 7), 3.25f)
        check(calculator, time(9, 18, 33), 0.39f)
        check(calculator, time(10, 1, 42), 3.25f, true)
        check(calculator, time(10, 8, 23), 0.4f)
        check(calculator, time(10, 14, 0), 2.71f, true)
        check(calculator, time(10, 19, 27), 0.39f, true)
        check(calculator, time(11, 2, 37), 2.54f)
        check(calculator, time(11, 9, 25), 0.39f)
        check(calculator, time(11, 14, 56), 2.54f, true)
        check(calculator, time(11, 20, 20), 0.4f, true)
        check(calculator, time(12, 3, 37), 3.07f)
        check(calculator, time(12, 10, 11), 0.56f)
    }

    private fun check(
        calculator: TideTableWaterLevelCalculator,
        time: ZonedDateTime,
        height: Float,
        exact: Boolean = false
    ) {
        assertEquals(height, calculator.calculate(time), if (exact) 0.0001f else 0.2f)
    }

    private fun time(day: Int, hour: Int, minute: Int): ZonedDateTime {
        return ZonedDateTime.of(
            LocalDate.of(2022, 1, day),
            LocalTime.of(hour, minute, 0),
            ZoneId.of("UTC")
        )
    }
}