package com.kylecorry.trail_sense.tools.packs.domain

import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class PackItemTest {

    @ParameterizedTest
    @MethodSource("providePackedWeight")
    fun getPackedWeight(
        amount: Double,
        weight: Float?,
        units: WeightUnits?,
        expected: Float?,
        expectedUnits: WeightUnits?
    ) {
        val item = PackItem(
            0,
            0,
            "",
            ItemCategory.Other,
            amount,
            weight = weight?.let { units?.let { Weight.from(weight, units) } })
        assertEquals(
            expected?.let { expectedUnits?.let { Weight.from(expected, expectedUnits) } },
            item.packedWeight
        )
    }

    @ParameterizedTest
    @MethodSource("provideDesiredWeight")
    fun getDesiredWeight(
        desiredAmount: Double,
        weight: Float?,
        units: WeightUnits?,
        expected: Float?,
        expectedUnits: WeightUnits?
    ) {
        val item =
            PackItem(
                0,
                0,
                "",
                ItemCategory.Other,
                desiredAmount = desiredAmount,
                weight = weight?.let {
                    units?.let { Weight.from(weight, units) }
                })
        assertEquals(
            expected?.let { expectedUnits?.let { Weight.from(expected, expectedUnits) } },
            item.desiredWeight
        )
    }

    @ParameterizedTest
    @MethodSource("providePercentPacked")
    fun getPercentPacked(amount: Double, desiredAmount: Double, expected: Float) {
        val item = PackItem(0, 0, "", ItemCategory.Other, amount, desiredAmount = desiredAmount)
        assertEquals(expected, item.percentPacked)
    }

    @ParameterizedTest
    @MethodSource("provideFullyPacked")
    fun isFullyPacked(amount: Double, desiredAmount: Double, expected: Boolean) {
        val item = PackItem(0, 0, "", ItemCategory.Other, amount, desiredAmount = desiredAmount)
        assertEquals(expected, item.isFullyPacked)
    }

    companion object {
        @JvmStatic
        fun providePackedWeight(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0.0, 1f, WeightUnits.Grams, 0f, WeightUnits.Grams),
                Arguments.of(1.0, 1f, WeightUnits.Grams, 1f, WeightUnits.Grams),
                Arguments.of(2.0, 1f, WeightUnits.Grams, 2f, WeightUnits.Grams),
                Arguments.of(1.0, null, null, null, null),
            )
        }

        @JvmStatic
        fun provideDesiredWeight(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0.0, 1f, WeightUnits.Grams, 0f, WeightUnits.Grams),
                Arguments.of(1.0, 1f, WeightUnits.Grams, 1f, WeightUnits.Grams),
                Arguments.of(2.0, 1f, WeightUnits.Grams, 2f, WeightUnits.Grams),
                Arguments.of(1.0, null, null, null, null),
            )
        }

        @JvmStatic
        fun providePercentPacked(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0.0, 1.0, 0f),
                Arguments.of(1.0, 1.0, 100f),
                Arguments.of(2.0, 1.0, 200f),
                Arguments.of(1.0, 2.0, 50f),
                Arguments.of(1.0, 0.0, 100f),
                Arguments.of(0.0, 0.0, 0f),
            )
        }

        @JvmStatic
        fun provideFullyPacked(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0.0, 1.0, false),
                Arguments.of(1.0, 1.0, true),
                Arguments.of(2.0, 1.0, true),
                Arguments.of(1.0, 2.0, false),
                Arguments.of(1.0, 0.0, true),
                Arguments.of(0.0, 0.0, false),
            )
        }
    }
}