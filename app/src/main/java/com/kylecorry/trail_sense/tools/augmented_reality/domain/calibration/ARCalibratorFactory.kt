package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate

class ARCalibratorFactory {

    private val astro = AstronomyService()

    suspend fun getSunCalibrator(location: Coordinate): IARCalibrator = onDefault {
        val position = astro.getSunPosition(location)
        ARCenteredCalibrator(
            AugmentedRealityCoordinate(
                AugmentedRealityUtils.toEastNorthUp(
                    position.azimuth.value,
                    position.altitude,
                    Float.MAX_VALUE
                ), true
            )
        )
    }

    suspend fun getMoonCalibrator(location: Coordinate): IARCalibrator = onDefault {
        val position = astro.getMoonPosition(location)
        ARCenteredCalibrator(
            AugmentedRealityCoordinate(
                AugmentedRealityUtils.toEastNorthUp(
                    position.azimuth.value,
                    position.altitude,
                    Float.MAX_VALUE
                ), true
            )
        )
    }

}
