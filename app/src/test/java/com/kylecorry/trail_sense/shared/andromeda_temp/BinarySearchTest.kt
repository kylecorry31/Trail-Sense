package com.kylecorry.trail_sense.shared.andromeda_temp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BinarySearchTest {

    @ParameterizedTest
    @CsvSource(
        "0, 0",
        "1, 0",
        "1.5, 1",
        "2, 1",
        "3, 4",
        "5, 4",
        "6, 5"
    )
    fun findsTheFirstIndexAtOrAbove(value: Double, expected: Int) {
        // The first of repeated values is found
        val sorted = doubleArrayOf(1.0, 2.0, 2.0, 2.0, 5.0)
        assertEquals(expected, BinarySearch.firstIndexAtOrAbove(sorted, value))
    }

    @ParameterizedTest
    @CsvSource("0, 10, 0", "10, 10, 10", "4, 10, 4", "0, 0, 0")
    fun findsTheFirstIndexWhereTheConditionHolds(firstTrue: Int, count: Int, expected: Int) {
        assertEquals(expected, BinarySearch.firstIndex(count) { it >= firstTrue })
    }
}
