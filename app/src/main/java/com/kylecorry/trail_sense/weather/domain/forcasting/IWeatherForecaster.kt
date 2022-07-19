package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading

interface IWeatherForecaster {

    fun forecast(readings: List<Reading<Pressure>>): Weather

}