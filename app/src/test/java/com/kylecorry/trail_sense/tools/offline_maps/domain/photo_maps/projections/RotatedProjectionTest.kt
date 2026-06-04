package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class RotatedProjectionTest {

    @ParameterizedTest
    @CsvSource(
        // 0 degrees
        "0, 10.0, 20.0, 0, 0",
        "0, 10.0, 30.0, 100, 0",
        "0, 0.0, 20.0, 0, 200",
        "0, 0.0, 30.0, 100, 200",
        "0, 5.0, 25.0, 50, 100",
        "0, 2.5, 22.5, 25, 150",
        "0, 7.5, 27.5, 75, 50",
        // 90 degrees
        "90, 10.0, 20.0, 0, 200",
        "90, 10.0, 30.0, 0, 100",
        "90, 0.0, 20.0, 200, 200",
        "90, 0.0, 30.0, 200, 100",
        "90, 5.0, 25.0, 100, 150",
        // 180 degrees
        "180, 10.0, 20.0, 100, 200",
        "180, 10.0, 30.0, 0, 200",
        "180, 0.0, 20.0, 100, 0",
        "180, 0.0, 30.0, 0, 0",
        "180, 5.0, 25.0, 50, 100",
        // 270 degrees
        "270, 10.0, 20.0, 100, 0",
        "270, 10.0, 30.0, 100, 100",
        "270, 0.0, 20.0, -100, 0",
        "270, 0.0, 30.0, -100, 100",
        "270, 5.0, 25.0, 0, 50",
        // 45 degrees
        "45, 10.0, 20.0, -100, 100",
        "45, 10.0, 30.0, -29.289322, 29.289322",
        "45, 0.0, 20.0, 41.421356, 241.42136",
        "45, 0.0, 30.0, 112.132034, 170.71068",
        "45, 5.0, 25.0, 6.066017, 135.35535"
    )
    fun canProject(
        rotation: Float,
        latitude: Double,
        longitude: Double,
        expectedX: Float,
        expectedY: Float
    ) {
        val projection = RotatedProjection(
            LinearProjection,
            Size(100f, 200f),
            rotation
        )
        val coordinate = Coordinate(latitude, longitude)
        val pixel = Vector2(expectedX, expectedY)

        assertPixels(pixel, projection.toPixels(coordinate))
        assertPixels(pixel, projection.toPixels(latitude, longitude))
        assertCoordinate(coordinate, projection.toCoordinate(pixel))
    }

    private fun assertCoordinate(expected: Coordinate, actual: Coordinate, tolerance: Double = 0.0001) {
        assertEquals(expected.latitude, actual.latitude, tolerance, "latitude")
        assertEquals(expected.longitude, actual.longitude, tolerance, "longitude")
    }

    private fun assertPixels(expected: Vector2, actual: Vector2, tolerance: Float = 0.001f) {
        assertEquals(expected.x, actual.x, tolerance, "x")
        assertEquals(expected.y, actual.y, tolerance, "y")
    }

    private object LinearProjection : IMapProjection {
        override fun toCoordinate(pixel: Vector2): Coordinate {
            return Coordinate(
                latitude = 10.0 - pixel.y / 20.0,
                longitude = 20.0 + pixel.x / 10.0
            )
        }

        override fun toPixels(location: Coordinate): Vector2 {
            return toPixels(location.latitude, location.longitude)
        }

        override fun toPixels(latitude: Double, longitude: Double): Vector2 {
            return Vector2(
                x = ((longitude - 20.0) * 10.0).toFloat(),
                y = ((10.0 - latitude) * 20.0).toFloat()
            )
        }
    }
}
