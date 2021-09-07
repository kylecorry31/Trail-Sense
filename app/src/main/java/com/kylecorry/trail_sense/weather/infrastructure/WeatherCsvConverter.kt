package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading

class WeatherCsvConverter {

    fun toCSV(readings: List<PressureAltitudeReading>): List<List<String>> {
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
                it.pressure.toString(),
                it.altitude.toString(),
                if (it.altitudeError != null) it.altitudeError.toString() else "",
                it.temperature.toString(),
                if (it.humidity != null) it.humidity.toString() else ""
            )
        }
    }

}