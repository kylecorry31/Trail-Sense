package com.kylecorry.trail_sense.tools.weather.infrastructure

import com.kylecorry.sol.units.Reading
import java.time.Instant
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation

class WeatherCsvConverter {

    fun toCSV(readings: List<Reading<RawWeatherObservation>>): List<List<String>> {
        val header = listOf(
            "Time",
            "Pressure (hPa)",
            "Elevation (m)",
            "Elevation Error (m)",
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

    fun fromCSV(csv: List<List<String>>): List<Reading<RawWeatherObservation>> {
        return csv.drop(1).mapNotNull { parseRow(it) }
    }

    private fun parseRow(row: List<String>): Reading<RawWeatherObservation>? {
        val time = row.getOrNull(0)?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: return null
        val pressure = row.getOrNull(1)?.toFloatOrNull() ?: return null
        val altitude = row.getOrNull(2)?.toFloatOrNull() ?: return null
        val temperature = row.getOrNull(4)?.toFloatOrNull() ?: return null
        val altitudeError = row.getOrNull(3)?.toFloatOrNull()
        val humidity = row.getOrNull(5)?.toFloatOrNull()

        return Reading(
            RawWeatherObservation(0, pressure, altitude, temperature, altitudeError, humidity),
            time
        )
    }

}
