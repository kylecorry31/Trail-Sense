package com.kylecorry.trail_sense.navigation.paths.domain

import android.graphics.Color
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ShouldLoadPathSpecificationTest {

    @ParameterizedTest
    @MethodSource("provideBounds")
    fun isSatisfiedBy(base: CoordinateBounds, pathBounds: CoordinateBounds, expected: Boolean) {
        val specification = ShouldLoadPathSpecification(base)

        val actual = specification.isSatisfiedBy(path(pathBounds))

        assertEquals(expected, actual)
    }

    companion object {
        @JvmStatic
        fun provideBounds(): Stream<Arguments> {
            val baseBounds = bounds(1.0, 1.0, 0.0, 0.0)

            return Stream.of(
                Arguments.of(baseBounds, baseBounds, true),
                Arguments.of(baseBounds, bounds(2.0, 2.0, -1.0, -1.0), true),
                Arguments.of(baseBounds, bounds(0.75, 0.75, 0.25, 0.25), true),
                Arguments.of(baseBounds, bounds(1.5, 1.5, 0.25, 0.25), true),
                Arguments.of(baseBounds, bounds(2.0, 2.0, 1.1, 1.1), false),
            )
        }

        private fun bounds(north: Double, east: Double, south: Double, west: Double): CoordinateBounds {
            return CoordinateBounds(north, east, south, west)
        }

        private fun path(bounds: CoordinateBounds): Path {
            return Path(
                1,
                null,
                PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true),
                PathMetadata(
                    Distance.meters(0f),
                    1,
                    null,
                    bounds
                )
            )
        }
    }

}