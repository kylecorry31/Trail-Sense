package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

internal class StrideLengthPaceCalculatorTest {

    @ParameterizedTest
    @MethodSource("provideDistance")
    fun distance(stride: Distance, steps: Long, expected: Distance) {
        val calculator = StrideLengthPaceCalculator(stride)
        val actual = calculator.distance(steps)
        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @MethodSource("provideSpeed")
    fun speed(stride: Distance, steps: Long, time: Duration, expected: Speed) {
        val calculator = StrideLengthPaceCalculator(stride)
        val actual = calculator.speed(steps, time)
        assertEquals(expected, actual)
    }


    companion object {
        @JvmStatic
        fun provideDistance(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Distance.meters(1f), 1000L, Distance.meters(1000f)),
                Arguments.of(Distance.feet(2f), 100L, Distance.feet(200f)),
                Arguments.of(Distance.meters(1f), 0L, Distance.meters(0f)),
            )
        }

        @JvmStatic
        fun provideSpeed(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Distance.meters(1f), 1000L, Duration.ofSeconds(1000), speed(Distance.meters(1f))),
                Arguments.of(Distance.feet(2f), 100L, Duration.ofSeconds(10), speed(Distance.feet(20f))),
                Arguments.of(Distance.meters(1f), 0L, Duration.ofSeconds(100), speed(Distance.meters(0f))),
                Arguments.of(Distance.meters(1f), 10L, Duration.ZERO, speed(Distance.meters(0f))),
            )
        }

        private fun speed(distancePerSecond: Distance): Speed {
            return Speed(distancePerSecond.distance, distancePerSecond.units, TimeUnits.Seconds)
        }
    }
}