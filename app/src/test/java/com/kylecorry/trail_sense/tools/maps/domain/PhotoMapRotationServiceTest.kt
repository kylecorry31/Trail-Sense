package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PhotoMapRotationServiceTest {

    // Write tests for calculating the size of the map under different rotations
    // Ex.
    // 100, 200, 90, 200, 100

    @ParameterizedTest
    @CsvSource(
        // Right angles
        "100, 200, 0, 100, 200",
        "100, 200, 90, 200, 100",
        "100, 200, 180, 100, 200",
        "100, 200, 270, 200, 100",
        // 45 degrees
        "100, 200, 45, 212.1320317678, 212.1320317678",
        "100, 200, 135, 212.1320317678, 212.1320317678",
        "100, 200, 225, 212.1320317678, 212.1320317678",
        "100, 200, 315, 212.1320317678, 212.1320317678",
        // 30 degrees
        "100, 200, 30, 186.6025433849, 223.20508043",
        "100, 200, 60, 223.20508043, 186.6025433849",
        "100, 200, 120, 223.20508043, 186.6025433849",
        "100, 200, 150, 186.6025433849, 223.20508043",
        "100, 200, 210, 186.6025433849, 223.20508043",
        "100, 200, 240, 223.20508043, 186.6025433849",
        "100, 200, 300, 223.20508043, 186.6025433849",
        "100, 200, 330, 186.6025433849, 223.20508043",
    )
    fun getSize(
        width: Float,
        height: Float,
        rotation: Int,
        expectedWidth: Float,
        expectedHeight: Float
    ) {
        val map = PhotoMap(
            0,
            "",
            "",
            MapCalibration.uncalibrated().copy(rotation = rotation),
            MapMetadata(Size(width, height), 0)
        )
        val service = PhotoMapRotationService(map)
        val size = service.getSize()
        assertEquals(expectedWidth, size.width, 0.0001f)
        assertEquals(expectedHeight, size.height, 0.0001f)
    }

    @ParameterizedTest
    @CsvSource(
        // Point is centered, rotation should not affect it
        "0.5, 0.5, 0, 0.5, 0.5",
        "0.5, 0.5, 90, 0.5, 0.5",
        "0.5, 0.5, 180, 0.5, 0.5",
        "0.5, 0.5, 270, 0.5, 0.5",
        // Point in corner
        "0, 0, 0, 0, 0",
        "0, 0, 90, 1, 0",
        "0, 0, 180, 1, 1",
        "0, 0, 270, 0, 1",
        // Point in opposite corner
        "1, 1, 0, 1, 1",
        "1, 1, 90, 0, 1",
        "1, 1, 180, 0, 0",
        "1, 1, 270, 1, 0",
        // Point in corner, 45 rotation
        "0, 0, 45, 0.666667, 0",
        "0, 0, 135, 1.0, 0.666667",
        "0, 0, 225, 0.333333, 1",
        "0, 0, 315, 0.0, 0.333333",
        // Point not in corner
        "0.25, 0.8, 45, 0.2166667, 0.6166667",
        "0.25, 0.8, 90, 0.2, 0.25",
        "0.25, 0.8, 135, 0.38333333, 0.21666667",
        "0.25, 0.8, 180, 0.75, 0.2",
        "0.25, 0.8, 225, 0.78333333, 0.38333333",
        "0.25, 0.8, 270, 0.8, 0.75",
        "0.25, 0.8, 315, 0.61666667, 0.78333333"
    )
    fun getCalibrationPoints(
        x: Float,
        y: Float,
        rotation: Int,
        expectedX: Float,
        expectedY: Float
    ) {
        val map = PhotoMap(
            0,
            "",
            "",
            MapCalibration(
                true,
                true,
                rotation,
                listOf(
                    MapCalibrationPoint(
                        Coordinate(0.0, 0.0),
                        PercentCoordinate(x, y)
                    )
                )
            ),
            MapMetadata(Size(100f, 200f), 0)
        )

        val service = PhotoMapRotationService(map)
        val points = service.getCalibrationPoints()

        assertEquals(expectedX, points[0].imageLocation.x, 0.0001f)
        assertEquals(expectedY, points[0].imageLocation.y, 0.0001f)

    }
}