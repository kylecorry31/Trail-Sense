package com.kylecorry.trail_sense.tools.maps.infrastructure.calibration

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class MapRotationCalculatorTest {

    @ParameterizedTest
    @CsvSource(
        // Location: 0 degrees, Pixel: 0 degrees to 315 degrees
        "0, 0, 1, 0, 200, 200, 200, 0, 0",
        "0, 0, 1, 0, 0, 200, 200, 0, 0",
        "0, 0, 1, 0, 0, 200, 0, 0, 0",
        "0, 0, 1, 0, 0, 0, 200, 200, 270",
        "0, 0, 1, 0, 200, 0, 200, 200, 180",
        "0, 0, 1, 0, 200, 0, 0, 200, 180",
        "0, 0, 1, 0, 200, 200, 0, 200, 90",
        "0, 0, 1, 0, 200, 200, 0, 0, 90",

        // Location: 45 degrees, Pixel: 0 degrees to 315 degrees
        "0, 0, 1, 1, 200, 200, 200, 0, 90",
        "0, 0, 1, 1, 0, 200, 200, 0, 0",
        "0, 0, 1, 1, 0, 200, 0, 0, 90",
        "0, 0, 1, 1, 0, 0, 200, 200, 270",
        "0, 0, 1, 1, 200, 0, 200, 200, 270",
        "0, 0, 1, 1, 200, 0, 0, 200, 180",
        "0, 0, 1, 1, 200, 200, 0, 200, 180",
        "0, 0, 1, 1, 200, 200, 0, 0, 90",

        // Location: 90 degrees, Pixel: 0 degrees to 315 degrees
        "0, 0, 0, 1, 200, 200, 200, 0, 90",
        "0, 0, 0, 1, 0, 200, 200, 0, 90",
        "0, 0, 0, 1, 0, 200, 0, 0, 90",
        "0, 0, 0, 1, 0, 0, 200, 200, 0",
        "0, 0, 0, 1, 200, 0, 200, 200, 270",
        "0, 0, 0, 1, 200, 0, 0, 200, 270",
        "0, 0, 0, 1, 200, 200, 0, 200, 180",
        "0, 0, 0, 1, 200, 200, 0, 0, 180",
    )
    fun calculate(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        expected: Int
    ) {

        val calibrationPoints = listOf(
            MapCalibrationPoint(
                Coordinate(latitude1, longitude1),
                PercentCoordinate(x1 / 400f, y1 / 200f)
            ),
            MapCalibrationPoint(
                Coordinate(latitude2, longitude2),
                PercentCoordinate(x2 / 400f, y2 / 200f)
            )
        )

        val map = PhotoMap(
            1,
            "",
            "",
            MapCalibration(true, false, 0, calibrationPoints),
            MapMetadata(Size(400f, 200f), 100, MapProjectionType.Mercator)
        )

        val rotation = MapRotationCalculator().calculate(map)

        assertEquals(expected, rotation)
    }
}