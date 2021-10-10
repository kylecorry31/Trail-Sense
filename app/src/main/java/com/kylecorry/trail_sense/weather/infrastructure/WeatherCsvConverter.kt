package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.WeatherObservation

class WeatherCsvConverter {

    fun toCSV(readings: List<Reading<WeatherObservation>>): List<List<String>> {
        val header = listOf(
            "Time",
            "Pressure (hPa)",
            "Altitude (m)",
            "Altitude Error (m)",
            "Temperature (C)",
            "Humidity (%)"
        )
        return listOf(header) + readings.map {
            listOf(
                it.time.toString(),
                it.value.pressure.toString(),
                it.value.altitude.toString(),
                if (it.value.altitudeError != null) it.value.altitudeError.toString() else "",
                it.value.temperature.toString(),
                if (it.value.humidity != null) it.value.humidity.toString() else ""
            )
        }
    }

}