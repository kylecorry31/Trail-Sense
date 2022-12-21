package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.shared.sensors.thermometer.ITemperatureCalibrator
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

interface IThermometerPreferences {
    val source: ThermometerSource
    var smoothing: Float
    var minBatteryTemperature: Float
    var minActualTemperature: Float
    var maxBatteryTemperature: Float
    var maxActualTemperature: Float
    var minBatteryTemperatureF: Float
    var minActualTemperatureF: Float
    var maxBatteryTemperatureF: Float
    var maxActualTemperatureF: Float
    val calibrator: ITemperatureCalibrator

    fun resetThermometerCalibration()
}