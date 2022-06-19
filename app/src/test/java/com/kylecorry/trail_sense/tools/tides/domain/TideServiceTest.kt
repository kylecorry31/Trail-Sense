package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Distance
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
        val table = TideTable(0, references.map { toMeters(it) }, isSemidiurnal = isSemidiurnal)
        val service = TideService()
        val actual = mutableListOf<Tide>()
        for (date in dates) {
            actual.addAll(service.getTides(table, date))
        }
        check(actual, expected.map { toMeters(it) }, expected.map { false })
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

            val maineAug1 = listOf(
                tide(2022, 8, 1, 1, 33, true, 11.12f),
                tide(2022, 8, 1, 7, 54, false, 0.36f),
                tide(2022, 8, 1, 14, 3, true, 10.48f),
                tide(2022, 8, 1, 20, 6, false, 1.24f),
            )

            val maineAug2 = listOf(
                tide(2022, 8, 2, 2, 11, true, 10.99f),
                tide(2022, 8, 2, 8, 30, false, 0.42f),
                tide(2022, 8, 2, 14, 40, true, 10.66f),
                tide(2022, 8, 2, 20, 48, false, 1.12f),
            )

            val maineAug3 = listOf(
                tide(2022, 8, 3, 2, 53, true, 10.79f),
                tide(2022, 8, 3, 9, 9, false, 0.52f),
                tide(2022, 8, 3, 15, 21, true, 10.84f),
                tide(2022, 8, 3, 21, 33, false, 0.99f),
            )

            val maineJan1 = listOf(
                tide(2023, 1, 1, 6, 14, true, 10.95f),
                tide(2023, 1, 1, 12, 39, false, 0.43f),
                tide(2023, 1, 1, 18, 48, true, 9.934f),
            )

            val maineJan2 = listOf(
                tide(2023, 1, 2, 0, 54, false, 0.95f),
                tide(2023, 1, 2, 7, 9, true, 11f),
                tide(2023, 1, 2, 13, 37, false, 0.27f),
                tide(2023, 1, 2, 19, 47, true, 9.83f),
            )

            val maineJune = maineJune1 + maineJune2 + maineJune3 + maineJune4
            val maineAug = maineAug1 + maineAug2 + maineAug3
            val maineJan = maineJan1 + maineJan2

            // Reference, date, expected
            return Stream.of(
                Arguments.of(
                    maineJune1,
                    dates(2022, 6, 1, 1),
                    maineJune1,
                    true
                ),
                Arguments.of(
                    maineJune1 + maineJune2,
                    dates(2022, 6, 1, 2),
                    maineJune1 + maineJune2,
                    true
                ),
                Arguments.of(
                    maineJune1.take(3) + maineJune2 + maineJune4,
                    dates(2022, 6, 1, 4),
                    maineJune,
                    true
                ),
                Arguments.of(
                    maineJune1 + maineJune2,
                    dates(2022, 6, 1, 4),
                    maineJune,
                    true
                ),
                Arguments.of(
                    maineJune1 + maineJune2,
                    dates(2022, 8, 1, 1),
                    maineAug1,
                    true
                ),
                Arguments.of(
                    maineJune1 + maineJune2 + maineAug + maineJan,
                    dates(2022, 6, 1, 2) + dates(2022, 8, 1, 3) + dates(2023, 1, 1, 2),
                    maineJune1 + maineJune2 + maineAug + maineJan,
                    true
                ),
                Arguments.of(
                    maineJune1.take(1),
                    dates(2022, 6, 1, 2),
                    (maineJune1 + maineJune2).map { it.copy(height = if (it.isHigh) it.height else 8.24f) },
                    true
                ),
            )
        }

        private fun toMeters(tide: Tide): Tide {
            return tide.copy(
                height = if (tide.height == null) null else Distance.feet(tide.height!!).meters().distance
            )
        }

        private fun dates(
            startYear: Int,
            startMonth: Int,
            startDay: Int,
            days: Int
        ): List<LocalDate> {
            val dates = mutableListOf<LocalDate>()
            val start = date(startYear, startMonth, startDay)
            dates.add(start)
            while (dates.size < days) {
                dates.add(start.plusDays(dates.size.toLong()))
            }
            return dates
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
                if (exact) 0.0001f else 4f
            )
            val delta = Duration.between(actual.time, expected.time).seconds / 60f
            Assertions.assertEquals(0f, delta, if (exact) 0f else 90f)
        }
    }

}