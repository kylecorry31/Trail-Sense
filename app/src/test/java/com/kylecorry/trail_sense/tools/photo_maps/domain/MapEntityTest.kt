package com.kylecorry.trail_sense.tools.photo_maps.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MapEntityTest {

    @Test
    fun toMapNormalizesCalibrationCoordinates() {
        val entity = MapEntity(
            name = "Test",
            filename = "test.webp",
            latitude1 = 95.0,
            longitude1 = 190.0,
            percentX1 = 0.25f,
            percentY1 = 0.75f,
            latitude2 = -95.0,
            longitude2 = -190.0,
            percentX2 = 0.75f,
            percentY2 = 0.25f,
            warped = false,
            rotated = false
        )

        val map = entity.toMap()

        assertEquals(90.0, map.calibration.calibrationPoints[0].location.latitude)
        assertEquals(-170.0, map.calibration.calibrationPoints[0].location.longitude)
        assertEquals(-90.0, map.calibration.calibrationPoints[1].location.latitude)
        assertEquals(170.0, map.calibration.calibrationPoints[1].location.longitude)
    }
}
