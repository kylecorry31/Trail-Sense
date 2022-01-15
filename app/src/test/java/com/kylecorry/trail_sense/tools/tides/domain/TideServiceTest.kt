package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.*

internal class TideServiceTest {

    @Test
    fun getTides() {
        val table = TideTable(
            0, listOf(
                Tide.high(time(10, 1, 42), 3.25f),
                Tide.high(time(10, 14, 0), 2.71f),
                Tide.low(time(10, 19, 27), 0.39f),
                Tide.high(time(11, 14, 56), 2.54f),
                Tide.low(time(11, 20, 20), 0.4f),
            )
        )

        val service = TideService()

        val tides9 = service.getTides(table, LocalDate.of(2022, 1, 9))
        val tides10 = service.getTides(table, LocalDate.of(2022, 1, 10))
        val tides11 = service.getTides(table, LocalDate.of(2022, 1, 11))
        val tides12 = service.getTides(table, LocalDate.of(2022, 1, 12))

        check(
            tides9,
            listOf(
                Tide.high(time(9, 0, 48), 3.39f),
                Tide.low(time(9, 6, 48), 0.53f),
                Tide.high(time(9, 13, 7), 3.25f),
                Tide.low(time(9, 18, 33), 0.39f),
            ),
            listOf(false, false, false, false)
        )

        check(
            tides10,
            listOf(
                Tide.high(time(10, 1, 42), 3.25f),
                Tide.low(time(10, 8, 23), 0.4f),
                Tide.high(time(10, 14, 0), 2.71f),
                Tide.low(time(10, 19, 27), 0.39f),
            ),
            listOf(true, false, true, true)
        )

        check(
            tides11,
            listOf(
                Tide.high(time(11, 2, 37), 2.54f),
                Tide.low(time(11, 9, 25), 0.39f),
                Tide.high(time(11, 14, 56), 2.54f),
                Tide.low(time(11, 20, 20), 0.4f),
            ),
            listOf(false, false, true, true)
        )

        check(
            tides12,
            listOf(
                Tide.high(time(12, 3, 37), 3.07f),
                Tide.low(time(12, 10, 11), 0.56f),
                Tide.high(time(12, 15, 56), 3.25f),
                Tide.low(time(12, 21, 11), 0.36f),
            ),
            listOf(false, false, false, false)
        )
    }

    private fun check(
        actual: List<Tide>,
        expected: List<Tide>,
        exact: List<Boolean>
    ) {
        Assertions.assertEquals(expected.size, actual.size)
        actual.zip(expected.zip(exact)).forEach {
            check(it.first, it.second.first, it.second.second)
        }
    }

    private fun check(
        actual: Tide,
        expected: Tide,
        exact: Boolean = false
    ) {
        Assertions.assertEquals(expected.isHigh, actual.isHigh)
        Assertions.assertEquals(expected.height!!, actual.height!!, if (exact) 0.0001f else 0.2f)
        val delta = Duration.between(actual.time, expected.time).seconds / 60f
        Assertions.assertEquals(0f, delta, if (exact) 0f else 90f)
    }

    private fun time(day: Int, hour: Int, minute: Int): ZonedDateTime {
        return ZonedDateTime.of(
            LocalDate.of(2022, 1, day),
            LocalTime.of(hour, minute, 0),
            ZoneId.systemDefault()
        )
    }
}