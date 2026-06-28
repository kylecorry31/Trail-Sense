package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PhotoMapProjectionTest {

    @ParameterizedTest
    @CsvSource(
        // 0 degrees
        "0, false, 10.0, 20.0, 0, 0",
        "0, false, 10.0, 30.0, 100, 0",
        "0, false, 0.0, 20.0, 0, 200",
        "0, false, 0.0, 30.0, 100, 200",
        "0, false, 5.0, 25.0, 50, 100",
        "0, false, 2.5, 22.5, 25, 150",
        "0, false, 7.5, 27.5, 75, 50",
        "0, true, 10.0, 20.0, 0, 0",
        "0, true, 0.0, 30.0, 100, 200",
        // 90 degrees, base rotation disabled
        "90, false, 10.0, 20.0, 0, 200",
        "90, false, 10.0, 30.0, 0, 0",
        "90, false, 0.0, 20.0, 100, 200",
        "90, false, 0.0, 30.0, 100, 0",
        // 90 degrees, base rotation enabled
        "90, true, 10.0, 20.0, 0, 0",
        "90, true, 10.0, 30.0, 200, 0",
        "90, true, 0.0, 20.0, 0, 100",
        "90, true, 0.0, 30.0, 200, 100",
        // 180 degrees
        "180, false, 10.0, 20.0, 100, 200",
        "180, true, 10.0, 20.0, 0, 0",
        // 270 degrees
        "270, false, 10.0, 20.0, 100, 0",
        "270, true, 10.0, 20.0, 0, 0",
        // 45 degrees, base rotation disabled
        "45, false, 10.0, 20.0, -100, 100",
        "45, false, 10.0, 30.0, 50, -50",
        "45, false, 0.0, 20.0, 50, 250",
        "45, false, 0.0, 30.0, 200, 100",
        "45, false, 5.0, 25.0, 50, 100",
        // 45 degrees, base rotation enabled
        "45, true, 10.0, 20.0, 100, -100",
        "45, true, 10.0, 30.0, 250, 50",
        "45, true, 0.0, 20.0, -50, 50",
        "45, true, 0.0, 30.0, 100, 200",
        "45, true, 5.0, 25.0, 100, 50"
    )
    fun canProject(
        rotation: Float,
        useBaseRotation: Boolean,
        latitude: Double,
        longitude: Double,
        expectedX: Float,
        expectedY: Float
    ) {
        val projection = PhotoMapProjection(
            testMap(rotation = rotation),
            useBaseRotation = useBaseRotation
        )
        val coordinate = Coordinate(latitude, longitude)
        val pixel = Vector2(expectedX, expectedY)

        assertPixels(pixel, projection.toPixels(coordinate))
        assertPixels(pixel, projection.toPixels(latitude, longitude))
        assertCoordinate(coordinate, projection.toCoordinate(pixel))
    }

    @ParameterizedTest
    @CsvSource(
        "true, 100, 200",
        "false, 50, 80"
    )
    fun usesConfiguredImageSize(
        usePdf: Boolean,
        expectedX: Float,
        expectedY: Float
    ) {
        val map = testMap(
            metadata = PhotoMapGeoreference(
                size = Size(100f, 200f),
                imageSize = Size(50f, 80f),
                projectionType = MapProjectionType.CylindricalEquidistant
            )
        )
        val projection = PhotoMapProjection(map, usePdf = usePdf)

        assertPixels(Vector2(expectedX, expectedY), projection.toPixels(southEast))
        assertCoordinate(southEast, projection.toCoordinate(Vector2(expectedX, expectedY)))
    }

    @Test
    fun returnsZeroesWhenCalibrationIsEmpty() {
        val projection = PhotoMapProjection(
            testMap(calibrationPoints = emptyList())
        )

        assertCoordinate(Coordinate.zero, projection.toCoordinate(Vector2(50f, 100f)))
        assertPixels(Vector2(0f, 0f), projection.toPixels(10.0, 20.0))
    }

    private fun testMap(
        rotation: Float = 0f,
        calibrationPoints: List<MapCalibrationPoint> = defaultCalibrationPoints,
        metadata: PhotoMapGeoreference = PhotoMapGeoreference(
            Size(100f, 200f),
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

    private fun assertCoordinate(expected: Coordinate, actual: Coordinate, tolerance: Double = 0.0001) {
        assertEquals(expected.latitude, actual.latitude, tolerance, "latitude")
        assertEquals(expected.longitude, actual.longitude, tolerance, "longitude")
    }

    private fun assertPixels(expected: Vector2, actual: Vector2, tolerance: Float = 0.001f) {
        assertEquals(expected.x, actual.x, tolerance, "x")
        assertEquals(expected.y, actual.y, tolerance, "y")
    }

    private companion object {
        val northWest: Coordinate = Coordinate(10.0, 20.0)
        val northEast: Coordinate = Coordinate(10.0, 30.0)
        val southWest: Coordinate = Coordinate(0.0, 20.0)
        val southEast: Coordinate = Coordinate(0.0, 30.0)

        val defaultCalibrationPoints: List<MapCalibrationPoint> = listOf(
            MapCalibrationPoint(northWest, PercentCoordinate(0f, 0f)),
            MapCalibrationPoint(northEast, PercentCoordinate(1f, 0f)),
            MapCalibrationPoint(southWest, PercentCoordinate(0f, 1f)),
            MapCalibrationPoint(southEast, PercentCoordinate(1f, 1f))
        )
    }
}
