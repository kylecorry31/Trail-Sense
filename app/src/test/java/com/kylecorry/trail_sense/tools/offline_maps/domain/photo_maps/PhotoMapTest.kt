package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
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
        "45, 5217.0303",
        "135, 5217.0303"
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

    private fun map(
        rotation: Float = 0f,
        calibrationPoints: List<MapCalibrationPoint> = defaultCalibrationPoints,
        metadata: PhotoMapGeoreference = PhotoMapGeoreference(
            size = Size(100f, 200f),
            projectionType = MapProjectionType.CylindricalEquidistant
        )
    ): PhotoMap {
        return PhotoMap(
            id = 0,
            name = "",
            files = listOf(OfflineMapFile("", 0, PhotoMap.FILE_ROLE_IMAGE)),
            georeference = metadata.copy(
                isWarpingCompleted = false,
                rotation = rotation,
                calibrationPoints = calibrationPoints
            )
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
