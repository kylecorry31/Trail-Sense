package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trailsensecore.domain.weather.IWeatherService
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.WeatherService

internal class AltimeterSeaLevelPressureConverter(
    private val altitudeCalculator: IAltitudeCalculator,
    private val useTemperature: Boolean
) : ISeaLevelPressureConverter {

    private val weatherService: IWeatherService = WeatherService()

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        val altitudeHistory = altitudeCalculator.convert(readings)

        return readings.mapIndexed { index, reading ->
            if (useTemperature) {
                weatherService.convertToSeaLevel(
                    reading.copy(altitude = altitudeHistory[index].value),
                    if (reading.temperature.isNaN()) 16f else reading.temperature
                )
            } else {
                weatherService.convertToSeaLevel(
                    reading.copy(altitude = altitudeHistory[index].value),
                    null
                )
            }
        }
    }

}