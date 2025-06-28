package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.calibration

import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class MapRotationCalculatorTest {

    @ParameterizedTest
    @CsvSource(
        // Location: 0 degrees, Pixel: 0 degrees to 315 degrees
        "0, 0, 1, 0, 200, 200, 200, 0, 0",
        "0, 0, 1, 0, 0, 200, 200, 0, 315",
        "0, 0, 1, 0, 0, 200, 0, 0, 0",
        "0, 0, 1, 0, 0, 0, 200, 200, 225",
        "0, 0, 1, 0, 200, 0, 200, 200, 180",
        "0, 0, 1, 0, 200, 0, 0, 200, 135",
        "0, 0, 1, 0, 200, 200, 0, 200, 90",
        "0, 0, 1, 0, 200, 200, 0, 0, 45",

        // Location: 45 degrees, Pixel: 0 degrees to 315 degrees
        "0, 0, 1, 1, 200, 200, 200, 0, 45",
        "0, 0, 1, 1, 0, 200, 200, 0, 0",
        "0, 0, 1, 1, 0, 200, 0, 0, 45",
        "0, 0, 1, 1, 0, 0, 200, 200, 270",
        "0, 0, 1, 1, 200, 0, 200, 200, 225",
        "0, 0, 1, 1, 200, 0, 0, 200, 180",
        "0, 0, 1, 1, 200, 200, 0, 200, 135",
        "0, 0, 1, 1, 200, 200, 0, 0, 90",

        // Location: 90 degrees, Pixel: 0 degrees to 315 degrees
        "0, 0, 0, 1, 200, 200, 200, 0, 90",
        "0, 0, 0, 1, 0, 200, 200, 0, 45",
        "0, 0, 0, 1, 0, 200, 0, 0, 90",
        "0, 0, 0, 1, 0, 0, 200, 200, 315",
        "0, 0, 0, 1, 200, 0, 200, 200, 270",
        "0, 0, 0, 1, 200, 0, 0, 200, 225",
        "0, 0, 0, 1, 200, 200, 0, 200, 180",
        "0, 0, 0, 1, 200, 200, 0, 0, 135",
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
        expected: Float
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
            MapCalibration(true, false, 0f, calibrationPoints),
            MapMetadata(Size(400f, 200f), null, 100, MapProjectionType.Mercator)
        )

        val rotation = MapRotationCalculator().calculate(map).roundPlaces(0) % 360f

        assertEquals(expected, rotation, 0.01f)
    }
}