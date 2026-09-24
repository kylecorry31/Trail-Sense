package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper.CameraAnglePixelMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CameraAnglePixelMapperTest {

    @Test
    fun sphericalAnglesRoundTripAwayFromCameraCenter() {
        val point = CameraAnglePixelMapper.toCartesian(45f, 30f, 1f)
        val angles = CameraAnglePixelMapper.toSpherical(point)

        assertEquals(30f, angles.y, 0.001f)
        assertEquals(45f, angles.z, 0.001f)
    }
}
