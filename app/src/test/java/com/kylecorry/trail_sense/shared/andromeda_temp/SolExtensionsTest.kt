package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.Range
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class SolExtensionsTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideMergeIntersectingRanges")
    fun mergeIntersecting(
        name: String,
        ranges: List<Range<Int>>,
        expected: List<Range<Int>>
    ) {
        assertEquals(expected, ranges.mergeIntersecting(), name)
    }

    companion object {
        @JvmStatic
        fun provideMergeIntersectingRanges(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "empty list",
                emptyList<Range<Int>>(),
                emptyList<Range<Int>>()
            ),
            Arguments.of(
                "single range",
                listOf(Range(1, 10)),
                listOf(Range(1, 10))
            ),
            Arguments.of(
                "disjoint ranges",
                listOf(Range(1, 3), Range(5, 7), Range(9, 11)),
                listOf(Range(1, 3), Range(5, 7), Range(9, 11))
            ),
            Arguments.of(
                "partially overlapping ranges",
                listOf(Range(1, 5), Range(3, 7)),
                listOf(Range(1, 7))
            ),
            Arguments.of(
                "overlapping ranges with second start before first start",
                listOf(Range(3, 7), Range(1, 5)),
                listOf(Range(1, 7))
            ),
            Arguments.of(
                "ranges touching at endpoint",
                listOf(Range(1, 5), Range(5, 10)),
                listOf(Range(1, 10))
            ),
            Arguments.of(
                "range containing next range",
                listOf(Range(1, 10), Range(3, 7)),
                listOf(Range(1, 10))
            ),
            Arguments.of(
                "ranges with equal starts",
                listOf(Range(1, 5), Range(1, 10)),
                listOf(Range(1, 10))
            ),
            Arguments.of(
                "identical ranges",
                listOf(Range(1, 5), Range(1, 5), Range(1, 5)),
                listOf(Range(1, 5))
            ),
            Arguments.of(
                "chain of overlapping ranges",
                listOf(Range(1, 4), Range(3, 7), Range(6, 10)),
                listOf(Range(1, 10))
            ),
            Arguments.of(
                "contained range between overlapping ranges",
                listOf(Range(1, 10), Range(3, 5), Range(8, 15)),
                listOf(Range(1, 15))
            ),
            Arguments.of(
                "multiple groups",
                listOf(
                    Range(1, 5),
                    Range(3, 7),
                    Range(10, 15),
                    Range(12, 20),
                    Range(25, 30)
                ),
                listOf(Range(1, 7), Range(10, 20), Range(25, 30))
            ),
            Arguments.of(
                "negative values",
                listOf(Range(-10, -5), Range(-7, 0), Range(1, 5)),
                listOf(Range(-10, 0), Range(1, 5))
            )
        )
    }
}
