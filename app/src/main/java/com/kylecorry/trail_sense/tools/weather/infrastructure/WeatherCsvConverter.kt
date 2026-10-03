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
        return csv.drop(1).mapNotNull { row ->
            try {
                Reading(
                    RawWeatherObservation(
                        0,
                        row[1].toFloat(),
                        row[2].toFloat(),
                        row[4].toFloat(),
                        row[3].toFloatOrNull(),
                        row.getOrNull(5)?.toFloatOrNull()
                    ),
                    Instant.parse(row[0])
                )
            } catch (e: Exception) {
                null
            }
        }
    }

}
