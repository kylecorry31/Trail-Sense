package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.weather.domain.PressureReading

data class CurrentWeather(
    val hourly: Weather,
    val daily: Weather,
    val tendency: PressureTendency,
    val pressure: PressureReading?,
    val temperature: Reading<Temperature>?,
    val humidity: Reading<Float>?
)