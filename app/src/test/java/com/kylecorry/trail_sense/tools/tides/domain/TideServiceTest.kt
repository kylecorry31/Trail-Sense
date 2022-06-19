package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.*
import java.util.stream.Stream

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

    @ParameterizedTest
    @MethodSource("provideTides")
    fun testRealWorldAccuracyHighLow(
        references: List<Tide>,
        dates: List<LocalDate>,
        expected: List<Tide>,
        isSemidiurnal: Boolean
    ) {
        val table = TideTable(0, references, isSemidiurnal = isSemidiurnal)
        val service = TideService()
        val actual = mutableListOf<Tide>()
        for (date in dates){
            actual.addAll(service.getTides(table, date))
        }
        check(actual, expected, expected.map { false })
    }

    companion object {
        @JvmStatic
        fun provideTides(): Stream<Arguments> {

            val maineJune1 = listOf(
                tide(2022, 6, 1, 0, 28, true, 11.24f),
                tide(2022, 6, 1, 6, 57, false, 0.26f),
                tide(2022, 6, 1, 13, 6, true, 10.02f),
                tide(2022, 6, 1, 19, 2, false, 1.74f),
            )

            val maineJune2 = listOf(
                tide(2022, 6, 2, 1, 6, true, 11.05f),
                tide(2022, 6, 2, 7, 35, false, 0.47f),
                tide(2022, 6, 2, 13, 44, true, 9.85f),
                tide(2022, 6, 2, 19, 40, false, 1.93f),
            )

            val maineJune3 = listOf(
                tide(2022, 6, 3, 1, 45, true, 10.84f),
                tide(2022, 6, 3, 8, 14, false, 0.69f),
                tide(2022, 6, 3, 14, 24, true, 9.7f),
                tide(2022, 6, 3, 20, 20, false, 2.08f),
            )

            val maineJune4 = listOf(
                tide(2022, 6, 4, 2, 25, true, 10.62f),
                tide(2022, 6, 4, 8, 54, false, 0.87f),
                tide(2022, 6, 4, 15, 5, true, 9.59f),
                tide(2022, 6, 4, 21, 3, false, 2.18f),
            )

            // Reference, date, expected
            return Stream.of(
                Arguments.of(
                    maineJune1,
                    listOf(date(2022, 6, 1)),
                    maineJune1,
                    true
                ),
                Arguments.of(
                    maineJune1 + maineJune2,
                    listOf(date(2022, 6, 1), date(2022, 6, 2)),
                    maineJune1 + maineJune2,
                    true
                ),
            )
        }

        private fun date(year: Int, month: Int, day: Int): LocalDate {
            return LocalDate.of(year, month, day)
        }

        private fun time(day: Int, hour: Int, minute: Int): ZonedDateTime {
            return ZonedDateTime.of(
                LocalDate.of(2022, 1, day),
                LocalTime.of(hour, minute, 0),
                ZoneId.systemDefault()
            )
        }

        private fun tide(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            high: Boolean,
            height: Float? = null,
            zone: String = "America/New_York"
        ): Tide {
            val time = ZonedDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute, 0),
                ZoneId.of(zone)
            )

            return Tide(time, high, height)
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
            Assertions.assertEquals(
                expected.height!!,
                actual.height!!,
                if (exact) 0.0001f else 0.2f
            )
            val delta = Duration.between(actual.time, expected.time).seconds / 60f
            Assertions.assertEquals(0f, delta, if (exact) 0f else 90f)
        }
    }

}