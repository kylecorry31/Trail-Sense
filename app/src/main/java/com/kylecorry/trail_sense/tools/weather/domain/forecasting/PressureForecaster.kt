package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.PressureTide
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation

class PressureForecaster(
    private val monteCarlo: MonteCarloPressureForecaster = MonteCarloPressureForecaster()
) {

    fun forecast(
        observations: List<WeatherObservation>,
        isPressureTrusted: Boolean
    ): List<Reading<Pressure>> {
        val recent = observations.takeLast(MAX_HISTORY)
        val location = recent.lastOrNull()?.location ?: return emptyList()
        val history = recent.map {
            PressureTide.remove(it.pressureReading(), it.location)
        }
        val error = if (isPressureTrusted) TRUSTED_ERROR else UNTRUSTED_ERROR
        return monteCarlo.getPressureForecast(
            history,
            maxErrorHpa = MAX_ERROR_HPA,
            velocityError = error,
            accelerationError = error
        ).map { PressureTide.add(it, location) }
    }

    companion object {
        private const val MAX_HISTORY = 50
        private const val MAX_ERROR_HPA = 8f
        private const val TRUSTED_ERROR = 0.1f
        private const val UNTRUSTED_ERROR = 0.2f
    }
}
