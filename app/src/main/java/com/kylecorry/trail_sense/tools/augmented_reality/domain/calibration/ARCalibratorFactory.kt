package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate

class ARCalibratorFactory {

    private val astro = AstronomyService()

    suspend fun getSunCalibrator(location: Coordinate): IARCalibrator = onDefault {
        ARCenteredCalibrator(
            AugmentedRealityCoordinate(
                AugmentedRealityUtils.toEastNorthUp(
                    astro.getSunAzimuth(location).value,
                    astro.getSunAltitude(location),
                    Float.MAX_VALUE
                ), true
            )
        )
    }

    suspend fun getMoonCalibrator(location: Coordinate): IARCalibrator = onDefault {
        ARCenteredCalibrator(
            AugmentedRealityCoordinate(
                AugmentedRealityUtils.toEastNorthUp(
                    astro.getMoonAzimuth(location).value,
                    astro.getMoonAltitude(location),
                    Float.MAX_VALUE
                ), true
            )
        )
    }

}