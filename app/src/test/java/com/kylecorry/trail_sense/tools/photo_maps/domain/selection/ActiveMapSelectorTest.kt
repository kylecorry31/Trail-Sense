package com.kylecorry.trail_sense.tools.photo_maps.domain.selection

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ActiveMapSelectorTest {

    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0, 4",
        "0.05, 0.05, 3",
        "0.1, 0.05, 3",
        "0.1, 0.1, 3",
        "0.2, 0.1, 1",
        "2.0, 0.1,",
    )
    fun canGetActiveMap(latitude: Double, longitude: Double, expected: Long?) {
        val selector = ActiveMapSelector()
        val maps = listOf(
            map(1, Size(100f, 100f), CoordinateBounds(0.0, 0.0, 1.0, 1.0)),
            map(2, Size(100f, 100f), CoordinateBounds(0.0, 0.0, 0.1, 0.1)),
            map(3, Size(400f, 400f), CoordinateBounds(0.0, 0.0, 0.1, 0.1)),
            map(4, Size(800f, 800f), CoordinateBounds(-0.1, -0.1, 0.1, 0.1)),
        )

        assertEquals(expected, selector.getActiveMap(maps, Coordinate(latitude, longitude))?.id)
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0, 0.1, 0.1, 3",
        "0.0, 0.0, 0.05, 0.05, 3",
        "0.05, 0.05, 0.1, 0.1, 3",
        "0.0, 0.0, 0.1, 0.11, 1"
    )
    fun canGetActiveMapWithDestination(
        latitude: Double,
        longitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double,
        expected: Long?
    ) {
        val selector = ActiveMapSelector()
        val maps = listOf(
            map(1, Size(500f, 500f), CoordinateBounds(0.0, 0.0, 0.15, 0.15)),
            map(2, Size(100f, 100f), CoordinateBounds(0.0, 0.0, 0.1, 0.1)),
            map(3, Size(400f, 400f), CoordinateBounds(0.0, 0.0, 0.1, 0.1)),
            map(4, Size(400f, 400f), CoordinateBounds(-0.1, -0.1, 0.1, 0.1)),
        )

        assertEquals(
            expected,
            selector.getActiveMap(
                maps,
                Coordinate(latitude, longitude),
                Coordinate(destinationLatitude, destinationLongitude)
            )?.id
        )
    }


    private fun map(id: Long, size: Size, boundary: CoordinateBounds): PhotoMap {
        return PhotoMap(
            id, "", "", MapCalibration(
                true,
                true,
                0f,
                listOf(
                    MapCalibrationPoint(boundary.northWest, PercentCoordinate(0f, 0f)),
                    MapCalibrationPoint(boundary.southEast, PercentCoordinate(1f, 1f))
                ),
            ), MapMetadata(
                size,
                null,
                0
            ), null
        )
    }

}