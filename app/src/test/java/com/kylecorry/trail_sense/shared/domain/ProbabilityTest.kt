package com.kylecorry.trail_sense.shared.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ProbabilityTest {

    @ParameterizedTest
    @MethodSource("provideProbability")
    fun probabilityTest(value: Float, expected: Probability) {
        assertEquals(expected, probability(value))
    }

    companion object {

        @JvmStatic
        fun provideProbability(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0f, Probability.Never),
                Arguments.of(0.04f, Probability.Never),
                Arguments.of(0.05f, Probability.Low),
                Arguments.of(0.24f, Probability.Low),
                Arguments.of(0.25f, Probability.Moderate),
                Arguments.of(0.74f, Probability.Moderate),
                Arguments.of(0.75f, Probability.High),
                Arguments.of(0.94f, Probability.High),
                Arguments.of(0.95f, Probability.Always),
                Arguments.of(1f, Probability.Always),
            )
        }
    }
}