package com.kylecorry.trail_sense.tools.packs.domain

import com.kylecorry.andromeda.core.units.Weight
import com.kylecorry.andromeda.core.units.WeightUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class PackServiceTest {

    @ParameterizedTest
    @MethodSource("providePackWeight")
    fun getPackWeight(items: List<PackItem>, units: WeightUnits, expected: Weight?) {
        val service = PackService()
        val actual = service.getPackWeight(items, units)
        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @MethodSource("providePercentPacked")
    fun getPercentPacked(items: List<PackItem>, expected: Float) {
        val service = PackService()
        val actual = service.getPercentPacked(items)
        assertEquals(expected, actual, 0.0001f)
    }

    @ParameterizedTest
    @MethodSource("provideFullyPacked")
    fun isFullyPacked(items: List<PackItem>, expected: Boolean) {
        val service = PackService()
        val actual = service.isFullyPacked(items)
        assertEquals(expected, actual)
    }

    companion object {

        private fun packItem(
            amount: Double,
            desiredAmount: Double = 0.0,
            weight: Weight? = null
        ): PackItem {
            return PackItem(0, 0, "", ItemCategory.Other, amount, desiredAmount, weight)
        }

        @JvmStatic
        fun providePercentPacked(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                        packItem(1.0, desiredAmount = 2.0),
                        packItem(3.0, desiredAmount = 3.0),
                    ),
                    50f
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                        packItem(1.0, desiredAmount = 0.0),
                        packItem(3.0, desiredAmount = 0.0),
                    ),
                    66.6666f
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                        packItem(0.0, desiredAmount = 0.0),
                        packItem(0.0, desiredAmount = 3.0),
                    ),
                    0f
                ),
                Arguments.of(
                    listOf<PackItem>(),
                    100f
                ),
                Arguments.of(
                    listOf(
                        packItem(1.0, desiredAmount = 1.0),
                    ),
                    100f
                ),
                Arguments.of(
                    listOf(
                        packItem(10.0, desiredAmount = 1.0),
                    ),
                    100f
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                    ),
                    0f
                ),
            )
        }

        @JvmStatic
        fun provideFullyPacked(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                        packItem(1.0, desiredAmount = 2.0),
                        packItem(3.0, desiredAmount = 3.0),
                    ),
                    false
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                        packItem(1.0, desiredAmount = 0.0),
                        packItem(3.0, desiredAmount = 0.0),
                    ),
                    false
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                        packItem(0.0, desiredAmount = 0.0),
                        packItem(0.0, desiredAmount = 3.0),
                    ),
                    false
                ),
                Arguments.of(
                    listOf<PackItem>(),
                    true
                ),
                Arguments.of(
                    listOf(
                        packItem(1.0, desiredAmount = 1.0),
                    ),
                    true
                ),
                Arguments.of(
                    listOf(
                        packItem(10.0, desiredAmount = 1.0),
                    ),
                    true
                ),
                Arguments.of(
                    listOf(
                        packItem(10.0, desiredAmount = 1.0),
                        packItem(1.0, desiredAmount = 1.0),
                        packItem(2.0, desiredAmount = 2.0),
                    ),
                    true
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0, desiredAmount = 1.0),
                    ),
                    false
                ),
            )
        }

        @JvmStatic
        fun providePackWeight(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    listOf(
                        packItem(0.0, weight = Weight(1f, WeightUnits.Pounds)),
                        packItem(1.0, weight = Weight(2f, WeightUnits.Pounds)),
                        packItem(2.0, weight = Weight(3f, WeightUnits.Pounds)),
                    ),
                    WeightUnits.Ounces,
                    Weight(128f, WeightUnits.Ounces)
                ),
                Arguments.of(
                    listOf(
                        packItem(1.0, weight = Weight(0.001f, WeightUnits.Kilograms)),
                        packItem(1.0),
                        packItem(2.0, weight = Weight(3f, WeightUnits.Pounds)),
                    ),
                    WeightUnits.Grams,
                    Weight(2722.552f, WeightUnits.Grams)
                ),
                Arguments.of(
                    listOf(
                        packItem(0.0),
                        packItem(1.0),
                        packItem(2.0),
                    ),
                    WeightUnits.Grams,
                    null
                ),
                Arguments.of(
                    listOf<PackItem>(),
                    WeightUnits.Grams,
                    null
                )
            )
        }
    }
}