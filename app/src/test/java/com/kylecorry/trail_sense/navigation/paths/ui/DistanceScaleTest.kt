package com.kylecorry.trail_sense.navigation.paths.ui

import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.paths.ui.DistanceScale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class DistanceScaleTest {

    @ParameterizedTest
    @MethodSource("provideScales")
    fun getScaleDistance(
        units: DistanceUnits,
        maxLength: Float,
        metersPerPixel: Float,
        expected: Float,
        expectedUnits: DistanceUnits
    ) {
        val scale = DistanceScale()
        val actual = scale.getScaleDistance(units, maxLength, metersPerPixel)
        assertEquals(expected, actual.value, 0.001f)
        assertEquals(expectedUnits, actual.units)
    }


    companion object {
        @JvmStatic
        fun provideScales(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(DistanceUnits.Meters, 200f, 1f, 200f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 199f, 1f, 100f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 201f, 1f, 200f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 600f, 1f, 500f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 1500f, 1f, 1f, DistanceUnits.Kilometers),

                Arguments.of(DistanceUnits.Meters, 200f, 0.5f, 100f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 199f, 0.5f, 50f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 201f, 0.5f, 100f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 600f, 0.5f, 200f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 1500f, 0.5f, 500f, DistanceUnits.Meters),

                Arguments.of(DistanceUnits.Meters, 200f, 2f, 200f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 199f, 2f, 200f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 201f, 2f, 200f, DistanceUnits.Meters),
                Arguments.of(DistanceUnits.Meters, 600f, 2f, 1f, DistanceUnits.Kilometers),
                Arguments.of(DistanceUnits.Meters, 1500f, 2f, 2f, DistanceUnits.Kilometers),


                Arguments.of(DistanceUnits.Feet, 200f, 0.333f, 200f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 150f, 0.333f, 100f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 201f, 0.333f, 200f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 600f, 0.333f, 500f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 1500f, 0.333f, 0.25f, DistanceUnits.Miles),

                Arguments.of(DistanceUnits.Feet, 200f, 0.167f, 100f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 150f, 0.167f, 50f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 201f, 0.167f, 100f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 600f, 0.167f, 200f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 1500f, 0.167f, 500f, DistanceUnits.Feet),

                Arguments.of(DistanceUnits.Feet, 200f, 0.667f, 200f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 199f, 0.667f, 200f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 201f, 0.667f, 200f, DistanceUnits.Feet),
                Arguments.of(DistanceUnits.Feet, 5280f, 0.667f, 2f, DistanceUnits.Miles),
                Arguments.of(DistanceUnits.Feet, 1500f, 0.667f, 0.5f, DistanceUnits.Miles),
            )
        }
    }

}