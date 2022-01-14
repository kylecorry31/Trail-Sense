package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.science.oceanography.Tide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.stream.Stream

internal class RuleOfTwelfthsWaterLevelCalculatorTest {

    @ParameterizedTest
    @MethodSource("provideWaterLevels")
    fun calculate(
        first: Tide,
        second: Tide,
        time: ZonedDateTime,
        expected: Float
    ) {
        val calculator = RuleOfTwelfthsWaterLevelCalculator(first, second)
        val actual = calculator.calculate(time)
        assertEquals(expected, actual, 0.01f)
    }

    companion object {
        @JvmStatic
        fun provideWaterLevels(): Stream<Arguments> {
            return Stream.of(
                // Low to high
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 0, 0),
                    0f
                ),
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 1, 0),
                    0.8f
                ),
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 2, 0),
                    3f
                ),
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 3, 0),
                    6f
                ),
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 4, 0),
                    9f
                ),
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 5, 0),
                    11.2f
                ),
                Arguments.of(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 6, 0),
                    12f
                ),
                // High to low
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 6, 0),
                    12f
                ),
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 7, 0),
                    11.2f
                ),
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 8, 0),
                    9f
                ),
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 9, 0),
                    6f
                ),
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 10, 0),
                    3f
                ),
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 11, 0),
                    0.8f
                ),
                Arguments.of(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 12, 0),
                    0f
                ),
            )
        }

        private fun time(day: Int, hour: Int, minute: Int): ZonedDateTime {
            return ZonedDateTime.of(
                LocalDate.of(2022, 1, day),
                LocalTime.of(hour, minute, 0),
                ZoneId.of("UTC")
            )
        }


    }
}