package com.kylecorry.trail_sense.weather.domain.sealevel.dwell

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.sealevel.ISeaLevelCalibrationStrategy

class DwellSeaLevelCalibrationStrategy(private val settings: DwellSeaLevelCalibrationSettings) :
    ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<PressureAltitudeReading>): List<PressureReading> {
        val seaLevelConverter = AltimeterSeaLevelPressureConverter(
            if (settings.pressureChangeThreshold != null) PressureDwellAltitudeCalculator(
                settings.dwellThreshold,
                settings.altitudeChangeThreshold, settings.pressureChangeThreshold / 3f
            ) else DwellAltitudeCalculator(
                settings.dwellThreshold,
                settings.altitudeChangeThreshold
            ),
            settings.useTemperature
        )
        return seaLevelConverter.convert(readings, settings.interpolateAltitudeChanges)
    }
}