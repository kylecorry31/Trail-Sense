package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.sealevel.kalman.KalmanSeaLevelCalibrationSettings
import com.kylecorry.trail_sense.weather.domain.sealevel.kalman.KalmanSeaLevelCalibrationStrategy
import com.kylecorry.trail_sense.weather.domain.sealevel.loess.LoessSeaLevelCalibrationStrategy

class SeaLevelCalibrationFactory {

    fun create(
        prefs: UserPreferences,
        seaLevelOverride: Boolean? = null
    ): ISeaLevelCalibrationStrategy {
        val useSeaLevel = prefs.weather.useSeaLevelPressure

        if (seaLevelOverride != null && seaLevelOverride != useSeaLevel) {
            return if (seaLevelOverride) {
                SimpleSeaLevelCalibrationStrategy(prefs.weather.seaLevelFactorInTemp)
            } else {
                NullSeaLevelCalibrationStrategy()
            }
        }

        if (!useSeaLevel) {
            return NullSeaLevelCalibrationStrategy()
        }

        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Override) {
            return SimpleSeaLevelCalibrationStrategy(prefs.weather.seaLevelFactorInTemp)
        }

        if (prefs.weather.useExperimentalSeaLevelCalibration){
            return LoessSeaLevelCalibrationStrategy(
                prefs.weather.altitudeSmoothing / 100f,
                prefs.weather.pressureSmoothing / 100f,
                prefs.weather.seaLevelFactorInTemp
            )
        }

        return KalmanSeaLevelCalibrationStrategy(
            KalmanSeaLevelCalibrationSettings(
                prefs.weather.altitudeOutlier,
                prefs.weather.altitudeSmoothing,
                prefs.weather.pressureSmoothing,
                prefs.weather.seaLevelFactorInTemp,
                prefs.weather.useAltitudeVariance
            )
        )
    }

}