package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.sealevel.dwell.DwellSeaLevelCalibrationSettings
import com.kylecorry.trail_sense.weather.domain.sealevel.dwell.DwellSeaLevelCalibrationStrategy
import com.kylecorry.trail_sense.weather.domain.sealevel.kalman.KalmanSeaLevelCalibrationSettings
import com.kylecorry.trail_sense.weather.domain.sealevel.kalman.KalmanSeaLevelCalibrationStrategy
import java.time.Duration

class SeaLevelCalibrationFactory {

    fun create(prefs: UserPreferences): ISeaLevelCalibrationStrategy {

        if (!prefs.weather.useSeaLevelPressure) {
            return NullSeaLevelCalibrationStrategy()
        }

        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Override) {
            return SimpleSeaLevelCalibrationStrategy(prefs.weather.seaLevelFactorInTemp)
        }

        if (prefs.weather.useExperimentalCalibration) {
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
        val dwellSettings = DwellSeaLevelCalibrationSettings(
            Duration.ofHours(1),
            prefs.weather.maxNonTravellingAltitudeChange,
            if (prefs.weather.seaLevelFactorInRapidChanges) prefs.weather.maxNonTravellingPressureChange else null,
            prefs.weather.seaLevelFactorInTemp,
            !prefs.weather.requireDwell
        )
        return DwellSeaLevelCalibrationStrategy(dwellSettings)
    }

}