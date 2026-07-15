package com.kylecorry.trail_sense.tools.pedometer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

internal class ActiveTimeCalculatorTest {

    private val calculator = ActiveTimeCalculator()

    @ParameterizedTest
    @MethodSource("provideActiveTimes")
    fun calculate(steps: Long, elapsedTime: Duration, expected: Duration) {
        assertEquals(expected, calculator.calculate(steps, elapsedTime))
    }

    companion object {
        @JvmStatic
        fun provideActiveTimes(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0L, Duration.ofSeconds(10), Duration.ZERO),
                Arguments.of(-1L, Duration.ofSeconds(10), Duration.ZERO),
                Arguments.of(10L, Duration.ZERO, Duration.ZERO),
                Arguments.of(10L, Duration.ofSeconds(-1), Duration.ZERO),
                Arguments.of(10L, Duration.ofSeconds(10), Duration.ofSeconds(10)),
                Arguments.of(10L, Duration.ofMinutes(1), Duration.ofSeconds(20)),
                Arguments.of(10L, Duration.ofSeconds(15), Duration.ofSeconds(15))
            )
        }
    }
}
