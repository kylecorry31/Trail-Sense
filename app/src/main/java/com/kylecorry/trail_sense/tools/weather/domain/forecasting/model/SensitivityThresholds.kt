package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity

internal data class SensitivityThresholds(val low: Float, val medium: Float, val high: Float) {
    fun get(sensitivity: ForecastSensitivity): Float {
        return when (sensitivity) {
            ForecastSensitivity.Low -> low
            ForecastSensitivity.Medium -> medium
            ForecastSensitivity.High -> high
        }
    }
}
