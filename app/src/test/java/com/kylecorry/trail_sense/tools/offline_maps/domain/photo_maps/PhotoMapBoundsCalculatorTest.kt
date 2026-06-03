package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PhotoMapBoundsCalculatorTest {

    private val calculator = PhotoMapBoundsCalculator()

    @ParameterizedTest
    @CsvSource(
        "0, 9.9999997, 30.0000008, 0.0, 19.9999994",
        "90, 9.9999997, 30.0000008, 0.0, 19.9999994",
        "180, 9.9999997, 30.0000008, 0.0, 19.9999994",
        "270, 9.9999997, 30.0000008, 0.0, 19.9999994",
        "45, 9.9999997, 30.0000008, -0.0000007, 19.9999994",
        "135, 9.9999997, 30.0000008, -0.0000007, 19.9999994"
    )
    fun usesCalibratedCorners(
        rotation: Float,
        expectedNorth: Double,
        expectedEast: Double,
        expectedSouth: Double,
        expectedWest: Double
    ) {
        val map = map(rotation = rotation)

        assertBounds(
            CoordinateBounds(expectedNorth, expectedEast, expectedSouth, expectedWest),
            calculator.calculate(map)
        )
    }

    @Test
    fun usesPdfSize() {
        val map = map(
            metadata = MapMetadata(
                size = Size(200f, 400f),
                unscaledPdfSize = null,
                fileSize = 0,
                projection = MapProjectionType.CylindricalEquidistant,
                imageSize = Size(100f, 200f)
            )
        )

        assertBounds(
            CoordinateBounds(9.9999997, 30.0000008, 0.0, 19.9999994),
            calculator.calculate(map)
        )
    }

    @Test
    fun returnsNullWhenUncalibrated() {
        val map = map(calibrationPoints = emptyList())

        assertNull(calculator.calculate(map))
    }

    @Test
    fun returnsWorldWhenFullWorld() {
        val map = map(isFullWorld = true)

        assertEquals(CoordinateBounds.world, calculator.calculate(map))
    }

    private fun assertBounds(expected: CoordinateBounds, actual: CoordinateBounds?) {
        assertEquals(expected.north, actual?.north ?: 0.0, 0.000001, "north")
        assertEquals(expected.east, actual?.east ?: 0.0, 0.000001, "east")
        assertEquals(expected.south, actual?.south ?: 0.0, 0.000001, "south")
        assertEquals(expected.west, actual?.west ?: 0.0, 0.000001, "west")
    }

    private fun map(
        rotation: Float = 0f,
        calibrationPoints: List<MapCalibrationPoint> = defaultCalibrationPoints,
        metadata: MapMetadata = MapMetadata(
            size = Size(100f, 200f),
            unscaledPdfSize = null,
            fileSize = 0,
            projection = MapProjectionType.CylindricalEquidistant
        ),
        isFullWorld: Boolean = false
    ): PhotoMap {
        return PhotoMap(
            id = 0,
            name = "",
            filename = "",
            calibration = MapCalibration(
                warped = false,
                rotated = rotation != 0f,
                rotation = rotation,
                calibrationPoints = calibrationPoints
            ),
            metadata = metadata,
            isFullWorld = isFullWorld
        )
    }

    private companion object {
        val northWest: Coordinate = Coordinate(10.0, 20.0)
        val northEast: Coordinate = Coordinate(10.0, 30.0)
        val southWest: Coordinate = Coordinate(0.0, 20.0)
        val southEast: Coordinate = Coordinate(0.0, 30.0)

        val defaultCalibrationPoints: List<MapCalibrationPoint> = listOf(
            MapCalibrationPoint(northWest, PercentCoordinate(0f, 0f)),
            MapCalibrationPoint(southEast, PercentCoordinate(1f, 1f)),
            MapCalibrationPoint(northEast, PercentCoordinate(1f, 0f)),
            MapCalibrationPoint(southWest, PercentCoordinate(0f, 1f))
        )
    }
}
