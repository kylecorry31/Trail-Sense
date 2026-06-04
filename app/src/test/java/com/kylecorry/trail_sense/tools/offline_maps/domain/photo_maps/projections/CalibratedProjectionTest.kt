package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class CalibratedProjectionTest {

    @ParameterizedTest
    @CsvSource(
        "10.0, 20.0, 0, 0",
        "10.0, 30.0, 100, 0",
        "0.0, 20.0, 0, 200",
        "0.0, 30.0, 100, 200",
        "5.0, 25.0, 50, 100",
        "2.5, 22.5, 25, 150",
        "7.5, 27.5, 75, 50"
    )
    fun canProjectWith4Points(
        latitude: Double,
        longitude: Double,
        expectedX: Float,
        expectedY: Float
    ) {
        val projection = CalibratedProjection(fourPointCalibration, LinearProjection)
        val coordinate = Coordinate(latitude, longitude)
        val pixel = Vector2(expectedX, expectedY)

        assertPixels(pixel, projection.toPixels(coordinate))
        assertPixels(pixel, projection.toPixels(latitude, longitude))
        assertCoordinate(coordinate, projection.toCoordinate(pixel))
    }

    @ParameterizedTest
    @CsvSource(
        "10.0, 20.0, 0, 0",
        "10.0, 30.0, 100, 0",
        "0.0, 20.0, 0, 200",
        "0.0, 30.0, 100, 200",
        "5.0, 25.0, 50, 100",
        "2.5, 22.5, 25, 150",
        "7.5, 27.5, 75, 50"
    )
    fun canProjectWith2Points(
        latitude: Double,
        longitude: Double,
        expectedX: Float,
        expectedY: Float
    ) {
        val projection = CalibratedProjection(twoPointCalibration, LinearProjection)
        val coordinate = Coordinate(latitude, longitude)
        val pixel = Vector2(expectedX, expectedY)

        assertPixels(pixel, projection.toPixels(coordinate))
        assertPixels(pixel, projection.toPixels(latitude, longitude))
        assertCoordinate(coordinate, projection.toCoordinate(pixel))
    }

    @Test
    fun returnsZeroesWhenCalibrationIsEmpty() {
        val projection = CalibratedProjection(emptyList(), LinearProjection)

        assertCoordinate(Coordinate.zero, projection.toCoordinate(Vector2(50f, 100f)))
        assertPixels(Vector2(0f, 0f), projection.toPixels(10.0, 20.0))
    }

    @Test
    fun returnsZeroesWhenCalibrationHasZeroWidth() {
        val projection = CalibratedProjection(
            listOf(
                PixelCoordinate(0f, 0f) to Coordinate(10.0, 20.0),
                PixelCoordinate(0f, 200f) to Coordinate(0.0, 20.0)
            ),
            LinearProjection
        )

        assertCoordinate(Coordinate.zero, projection.toCoordinate(Vector2(0f, 100f)))
        assertPixels(Vector2(0f, 0f), projection.toPixels(5.0, 20.0))
    }

    @Test
    fun returnsZeroesWhenCalibrationHasZeroHeight() {
        val projection = CalibratedProjection(
            listOf(
                PixelCoordinate(0f, 0f) to Coordinate(10.0, 20.0),
                PixelCoordinate(100f, 0f) to Coordinate(10.0, 30.0)
            ),
            LinearProjection
        )

        assertCoordinate(Coordinate.zero, projection.toCoordinate(Vector2(50f, 0f)))
        assertPixels(Vector2(0f, 0f), projection.toPixels(10.0, 25.0))
    }

    private fun assertCoordinate(expected: Coordinate, actual: Coordinate, tolerance: Double = 0.0001) {
        assertEquals(expected.latitude, actual.latitude, tolerance, "latitude")
        assertEquals(expected.longitude, actual.longitude, tolerance, "longitude")
    }

    private fun assertPixels(expected: Vector2, actual: Vector2, tolerance: Float = 0.001f) {
        assertEquals(expected.x, actual.x, tolerance, "x")
        assertEquals(expected.y, actual.y, tolerance, "y")
    }

    private companion object {
        val fourPointCalibration: List<Pair<PixelCoordinate, Coordinate>> = listOf(
            PixelCoordinate(0f, 0f) to Coordinate(10.0, 20.0),
            PixelCoordinate(100f, 0f) to Coordinate(10.0, 30.0),
            PixelCoordinate(0f, 200f) to Coordinate(0.0, 20.0),
            PixelCoordinate(100f, 200f) to Coordinate(0.0, 30.0)
        )

        val twoPointCalibration: List<Pair<PixelCoordinate, Coordinate>> = listOf(
            PixelCoordinate(0f, 0f) to Coordinate(10.0, 20.0),
            PixelCoordinate(100f, 200f) to Coordinate(0.0, 30.0)
        )
    }

    private object LinearProjection : IMapProjection {
        override fun toCoordinate(pixel: Vector2): Coordinate {
            return Coordinate(
                latitude = pixel.y.toDouble(),
                longitude = pixel.x.toDouble()
            )
        }

        override fun toPixels(location: Coordinate): Vector2 {
            return toPixels(location.latitude, location.longitude)
        }

        override fun toPixels(latitude: Double, longitude: Double): Vector2 {
            return Vector2(longitude.toFloat(), latitude.toFloat())
        }
    }
}
