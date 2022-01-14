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
import kotlin.math.sqrt

internal class TideClockWaterLevelCalculatorTest {

    @ParameterizedTest
    @MethodSource("provideTideClock")
    fun calculate(
        reference: Tide,
        frequency: Float,
        amplitude: Float,
        z0: Float,
        time: ZonedDateTime,
        expected: Float
    ) {
        val calculator = TideClockWaterLevelCalculator(reference, frequency, amplitude, z0)
        val actual = calculator.calculate(time)
        assertEquals(expected, actual, 0.01f)
    }


    companion object {
        @JvmStatic
        fun provideTideClock(): Stream<Arguments> {
            return Stream.of(
                // High reference
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 0), 1f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 0), -1f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 2, 0), 1f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 30), 0f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 30), 0f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(9, 23, 0), -1f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(9, 22, 0), 1f),

                // Low reference
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 0), -1f),
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 0), 1f),
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 2, 0), -1f),
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 30), 0f),
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 30), 0f),
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(9, 23, 0), 1f),
                Arguments.of(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(9, 22, 0), -1f),

                // Z0
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 5f, time(10, 0, 0), 6f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 5f, time(10, 1, 0), 4f),

                // Amplitude
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 2f, 0f, time(10, 0, 0), 2f),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 2f, 0f, time(10, 1, 0), -2f),

                // Different frequency
                Arguments.of(Tide.high(time(10, 0, 0)), 90f, 1f, 0f, time(10, 0, 0), 1f),
                Arguments.of(Tide.high(time(10, 0, 0)), 90f, 1f, 0f, time(10, 1, 0), 0f),
                Arguments.of(Tide.high(time(10, 0, 0)), 90f, 1f, 0f, time(10, 2, 0), -1f),

                // Intermediate
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 15), sqrt(2f) / 2),
                Arguments.of(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 15), -sqrt(2f) / 2),

                // Different reference date
                Arguments.of(Tide.high(time(1, 0, 0)), 90f, 1f, 0f, time(2, 0, 0), 1f),
                Arguments.of(Tide.high(time(1, 0, 0)), 90f, 1f, 0f, time(2, 1, 0), 0f),
                Arguments.of(Tide.high(time(1, 0, 0)), 90f, 1f, 0f, time(2, 2, 0), -1f),
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