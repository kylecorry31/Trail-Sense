package com.kylecorry.trail_sense.tools.clock.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

internal class ClockOffsetTest {

    @ParameterizedTest
    @MethodSource("provideClockOffsets")
    fun getClockOffset(duration: Duration, expected: ClockOffset) {
        assertEquals(expected, getClockOffset(duration))
    }

    companion object {
        @JvmStatic
        fun provideClockOffsets(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    Duration.ofMillis(2500),
                    ClockOffset(2, ClockOffsetDirection.Slow)
                ),
                Arguments.of(
                    Duration.ofMillis(-2500),
                    ClockOffset(2, ClockOffsetDirection.Fast)
                ),
                Arguments.of(Duration.ZERO, ClockOffset(0, ClockOffsetDirection.Accurate)),
                Arguments.of(
                    Duration.ofMillis(-500),
                    ClockOffset(0, ClockOffsetDirection.Accurate)
                )
            )
        }
    }
}
