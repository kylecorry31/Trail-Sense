package com.kylecorry.trail_sense.tools.weather.benchmark

import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricTemperatureRepo
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.HistoricTemperatureService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZoneOffset

@Tag("manual")
internal class ClimateFeatureExport {

    @Test
    fun export() = runBlocking {
        val normals = BenchmarkStation.loadClimateNormals()
        val builder = StringBuilder("id,date,low_c,high_c\n")
        for (station in BenchmarkStation.loadAll()) {
            val service = HistoricTemperatureService(
                HistoricTemperatureRepo { normals.getValue(station.id) },
                station.location,
                station.elevation
            )
            val years = station.observations.map { it.time.atZone(ZoneOffset.UTC).year }.distinct()
            for (year in years) {
                for ((date, range) in service.getTemperatureRanges(year)) {
                    builder.append("${station.id},$date,${range.start.celsius().value},${range.end.celsius().value}\n")
                }
            }
        }
        File("build/reports/weather-benchmark").apply { mkdirs() }.resolve("climate_features.csv").writeText(builder.toString())
    }
}
