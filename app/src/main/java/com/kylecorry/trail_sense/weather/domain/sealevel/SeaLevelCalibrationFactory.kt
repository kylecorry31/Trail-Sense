package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.shared.UserPreferences

class SeaLevelCalibrationFactory {

    fun create(
        prefs: UserPreferences,
        seaLevelOverride: Boolean? = null
    ): ISeaLevelCalibrationStrategy {
        val useSeaLevel = prefs.weather.useSeaLevelPressure

        val baseStrategy = if (useSeaLevel || seaLevelOverride == true) {
            SimpleSeaLevelCalibrationStrategy(prefs.weather.seaLevelFactorInTemp)
        } else {
            NullSeaLevelCalibrationStrategy()
        }

        return LoessSeaLevelCalibrationStrategy(
            baseStrategy,
            prefs.weather.pressureSmoothing / 100f
        )
    }

}