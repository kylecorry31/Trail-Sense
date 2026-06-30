package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.CylindricalEquidistantProjection
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    @Test
    fun doesNotCollapseCoordinatesWhenProjectedBoundsAreVerySmall() {
        val projection = CalibratedProjection(
            listOf(
                PixelCoordinate(0f, 0f) to Coordinate(0.0001, 0.0),
                PixelCoordinate(1000f, 1000f) to Coordinate(0.0, 0.0001)
            ),
            CylindricalEquidistantProjection()
        )

        val first = projection.toPixels(0.00005, 0.00005)
        val second = projection.toPixels(0.0000499, 0.0000501)

        assertNotEquals(first, second)
        assertPixels(Vector2(500f, 500f), first)
        assertPixels(Vector2(501f, 501f), second)
        assertCoordinate(Coordinate(0.00005, 0.00005), projection.toCoordinate(first), 0.0000001)
        assertCoordinate(Coordinate(0.0000499, 0.0000501), projection.toCoordinate(second), 0.0000001)
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0, 180, 80",
        "40.0, -90.0, 90, 40",
        "-40.0, 90.0, 270, 120"
    )
    fun canProjectFullWorldBounds(
        latitude: Double,
        longitude: Double,
        expectedX: Float,
        expectedY: Float
    ) {
        val projection = CalibratedProjection(fullWorldCalibration, LinearProjection)
        val coordinate = Coordinate(latitude, longitude)
        val pixel = Vector2(expectedX, expectedY)

        assertPixels(pixel, projection.toPixels(coordinate))
        assertPixels(pixel, projection.toPixels(latitude, longitude))
        assertCoordinate(coordinate, projection.toCoordinate(pixel))
    }

    @Test
    fun canProjectFullWorldBoundsAtDateLine() {
        val projection = CalibratedProjection(fullWorldCalibration, LinearProjection)

        assertPixels(Vector2(0f, 0f), projection.toPixels(80.0, -180.0))
        assertPixels(Vector2(360f, 0f), projection.toPixels(80.0, 180.0))
        assertPixels(Vector2(0f, 160f), projection.toPixels(-80.0, -180.0))
        assertPixels(Vector2(360f, 160f), projection.toPixels(-80.0, 180.0))
    }

    @ParameterizedTest
    @CsvSource(
        "10.0, 170.0, 0, 0",
        "10.0, -170.0, 100, 0",
        "0.0, 170.0, 0, 200",
        "0.0, -170.0, 100, 200",
        "5.0, 180.0, 50, 100",
        "5.0, -175.0, 75, 100",
        "5.0, 175.0, 25, 100"
    )
    fun canProjectBoundsCrossingAntimeridian(
        latitude: Double,
        longitude: Double,
        expectedX: Float,
        expectedY: Float
    ) {
        val projection = CalibratedProjection(
            antimeridianCrossingCalibration,
            LongitudeWrappingLinearProjection
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

        val fullWorldCalibration: List<Pair<PixelCoordinate, Coordinate>> = listOf(
            PixelCoordinate(0f, 0f) to Coordinate(80.0, -180.0),
            PixelCoordinate(360f, 0f) to Coordinate(80.0, 180.0),
            PixelCoordinate(0f, 160f) to Coordinate(-80.0, -180.0),
            PixelCoordinate(360f, 160f) to Coordinate(-80.0, 180.0)
        )

        val antimeridianCrossingCalibration: List<Pair<PixelCoordinate, Coordinate>> = listOf(
            PixelCoordinate(0f, 0f) to Coordinate(10.0, 170.0),
            PixelCoordinate(100f, 0f) to Coordinate(10.0, -170.0),
            PixelCoordinate(0f, 200f) to Coordinate(0.0, 170.0),
            PixelCoordinate(100f, 200f) to Coordinate(0.0, -170.0)
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

    private object LongitudeWrappingLinearProjection : IMapProjection {
        override fun toCoordinate(pixel: Vector2): Coordinate {
            return Coordinate(
                latitude = pixel.y.toDouble(),
                longitude = if (pixel.x > 180f) {
                    pixel.x.toDouble() - 360.0
                } else {
                    pixel.x.toDouble()
                }
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
