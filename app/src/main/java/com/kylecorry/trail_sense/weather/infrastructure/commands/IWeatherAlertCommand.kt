package com.kylecorry.trail_sense.weather.infrastructure.commands

import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather

interface IWeatherAlertCommand {

    fun execute(weather: CurrentWeather)

}