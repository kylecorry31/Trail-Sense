package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.MapProjectionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PhotoMapTest {

    @ParameterizedTest
    @CsvSource(
        "0, 6999.381",
        "90, 6999.381",
        "180, 6999.381",
        "270, 6999.381",
        "45, 6999.381",
        "135, 6999.381"
    )
    fun distancePerPixelUsesCalibratedPixelDistance(
        rotation: Float,
        expectedMetersPerPixel: Float
    ) {
        val map = map(rotation = rotation)

        assertEquals(
            expectedMetersPerPixel,
            map.distancePerPixel()!!.meters().value,
            0.001f
        )
    }

    @Test
    fun distancePerPixelUsesPdfSize() {
        val map = map(
            metadata = PhotoMapGeoreference(
                size = Size(200f, 400f),
                imageSize = Size(100f, 200f),
                projectionType = MapProjectionType.CylindricalEquidistant
            )
        )

        assertEquals(
            3499.6904f,
            map.distancePerPixel()!!.meters().value,
            0.001f
        )
    }

    @Test
    fun distancePerPixelIsNullWhenUncalibrated() {
        val map = map(calibrationPoints = emptyList())

        assertNull(map.distancePerPixel())
    }

    @ParameterizedTest
    @CsvSource(
        "0, 12.0, 30.0, -2.0, 20.0",
        "90, 12.0, 30.0, -2.0, 20.0",
        "180, 12.0, 30.0, -2.0, 20.0",
        "270, 12.0, 30.0, -2.0, 20.0",
        "45, 12.0, 30.0, -2.0, 20.0",
        "135, 12.0, 30.0, -2.0, 20.0"
    )
    fun boundsUseCalibratedCorners(
        rotation: Float,
        expectedNorth: Double,
        expectedEast: Double,
        expectedSouth: Double,
        expectedWest: Double
    ) {
        val map = map(rotation = rotation)

        assertBounds(
            CoordinateBounds(expectedNorth, expectedEast, expectedSouth, expectedWest),
            map.bounds
        )
    }

    @Test
    fun boundsUsePdfSize() {
        val map = map(
            metadata = PhotoMapGeoreference(
                size = Size(200f, 400f),
                imageSize = Size(100f, 200f),
                projectionType = MapProjectionType.CylindricalEquidistant
            )
        )

        assertBounds(
            CoordinateBounds(12.0, 30.0, -2.0, 20.0),
            map.bounds
        )
    }

    @Test
    fun boundsAreNullWhenUncalibrated() {
        val map = map(calibrationPoints = emptyList())

        assertNull(map.bounds)
    }

    @Test
    fun boundsAreWorldWhenFullWorld() {
        val map = map(isFullWorld = true)

        assertEquals(CoordinateBounds.world, map.bounds)
    }

    private fun assertBounds(expected: CoordinateBounds, actual: CoordinateBounds?) {
        assertEquals(expected.north, actual?.north ?: 0.0, 0.00001, "north")
        assertEquals(expected.east, actual?.east ?: 0.0, 0.00001, "east")
        assertEquals(expected.south, actual?.south ?: 0.0, 0.00001, "south")
        assertEquals(expected.west, actual?.west ?: 0.0, 0.00001, "west")
    }

    private fun map(
        rotation: Float = 0f,
        calibrationPoints: List<MapCalibrationPoint> = defaultCalibrationPoints,
        metadata: PhotoMapGeoreference = PhotoMapGeoreference(
            size = Size(100f, 200f),
            projectionType = MapProjectionType.CylindricalEquidistant
        ),
        isFullWorld: Boolean = false
    ): PhotoMap {
        return PhotoMap(
            id = 0,
            name = "",
            files = listOf(OfflineMapFile("", 0, PhotoMap.FILE_ROLE_IMAGE)),
            georeference = metadata.copy(
                isWarpingCompleted = false,
                rotation = rotation,
                calibrationPoints = calibrationPoints,
                isFullWorld = isFullWorld
            )
        )
    }

    private companion object {
        val northWest: Coordinate = Coordinate(10.0, 20.0)
        val southEast: Coordinate = Coordinate(0.0, 30.0)

        val defaultCalibrationPoints: List<MapCalibrationPoint> = listOf(
            MapCalibrationPoint(northWest, PercentCoordinate(0f, 0f)),
            MapCalibrationPoint(southEast, PercentCoordinate(1f, 1f))
        )
    }
}
