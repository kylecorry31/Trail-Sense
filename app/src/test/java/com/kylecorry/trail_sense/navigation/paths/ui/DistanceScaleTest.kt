package com.kylecorry.trail_sense.navigation.paths.ui

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class DistanceScaleTest {

    @ParameterizedTest
    @MethodSource("provideScales")
    fun getScaleDistance(units: DistanceUnits, maxLength: Float, metersPerPixel: Float, expected: Distance) {
        val scale = DistanceScale()
        val actual = scale.getScaleDistance(units, maxLength, metersPerPixel)
        assertEquals(expected.distance, actual.distance, 0.001f)
        assertEquals(expected.units, actual.units)
    }


    companion object {
        @JvmStatic
        fun provideScales(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(DistanceUnits.Meters, 200f, 1f, Distance(200f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 199f, 1f, Distance(100f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 201f, 1f, Distance(200f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 600f, 1f, Distance(500f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 1500f, 1f, Distance(1f, DistanceUnits.Kilometers)),

                Arguments.of(DistanceUnits.Meters, 200f, 0.5f, Distance(100f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 199f, 0.5f, Distance(50f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 201f, 0.5f, Distance(100f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 600f, 0.5f, Distance(200f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 1500f, 0.5f, Distance(500f, DistanceUnits.Meters)),

                Arguments.of(DistanceUnits.Meters, 200f, 2f, Distance(200f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 199f, 2f, Distance(200f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 201f, 2f, Distance(200f, DistanceUnits.Meters)),
                Arguments.of(DistanceUnits.Meters, 600f, 2f, Distance(1f, DistanceUnits.Kilometers)),
                Arguments.of(DistanceUnits.Meters, 1500f, 2f, Distance(2f, DistanceUnits.Kilometers)),


                Arguments.of(DistanceUnits.Feet, 200f, 0.333f, Distance(200f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 150f, 0.333f, Distance(100f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 201f, 0.333f, Distance(200f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 600f, 0.333f, Distance(500f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 1500f, 0.333f, Distance(0.25f, DistanceUnits.Miles)),

                Arguments.of(DistanceUnits.Feet, 200f, 0.167f, Distance(100f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 150f, 0.167f, Distance(50f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 201f, 0.167f, Distance(100f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 600f, 0.167f, Distance(200f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 1500f, 0.167f, Distance(500f, DistanceUnits.Feet)),

                Arguments.of(DistanceUnits.Feet, 200f, 0.667f, Distance(200f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 199f, 0.667f, Distance(200f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 201f, 0.667f, Distance(200f, DistanceUnits.Feet)),
                Arguments.of(DistanceUnits.Feet, 5280f, 0.667f, Distance(2f, DistanceUnits.Miles)),
                Arguments.of(DistanceUnits.Feet, 1500f, 0.667f, Distance(0.5f, DistanceUnits.Miles)),
            )
        }
    }

}