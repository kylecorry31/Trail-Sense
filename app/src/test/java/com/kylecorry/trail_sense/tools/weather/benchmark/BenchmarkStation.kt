package com.kylecorry.trail_sense.tools.weather.benchmark

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import java.time.Instant
import java.time.Month
import java.util.zip.GZIPInputStream

internal data class BenchmarkObservation(
    val time: Instant,
    val pressureHpa: Float?,
    val temperatureC: Float?,
    val humidityPercent: Float?,
    val precipitationMm: Float?,
    val weatherCodes: List<String>,
    val gustKnots: Float?,
    val windKnots: Float?,
    val skyCover: String?
) {
    private val presentWeather = weatherCodes.filterNot { it.startsWith("VC") }

    val isWet: Boolean
        get() = (precipitationMm ?: 0f) >= MEASURABLE_PRECIPITATION_MM ||
            presentWeather.any { code -> PRECIPITATION_CODES.any { code.contains(it) } }

    val isSnow: Boolean
        get() = presentWeather.any { code -> FROZEN_CODES.any { code.contains(it) } }

    val isThunder: Boolean
        get() = presentWeather.any { it.contains("TS") }

    val isStormy: Boolean
        get() = isThunder || (gustKnots ?: 0f) >= STORM_GUST_KNOTS ||
            (precipitationMm ?: 0f) >= HEAVY_PRECIPITATION_MM

    val isWindy: Boolean
        get() = (gustKnots ?: 0f) >= WINDY_GUST_KNOTS || (windKnots ?: 0f) >= WINDY_SUSTAINED_KNOTS

    val isClearSky: Boolean
        get() = skyCover in CLEAR_COVERS

    val isOvercastSky: Boolean
        get() = skyCover in OVERCAST_COVERS

    companion object {
        private const val MEASURABLE_PRECIPITATION_MM = 0.25f
        private const val HEAVY_PRECIPITATION_MM = 6f
        private const val STORM_GUST_KNOTS = 35f
        private const val WINDY_GUST_KNOTS = 25f
        private const val WINDY_SUSTAINED_KNOTS = 20f
        private val PRECIPITATION_CODES = listOf("RA", "SN", "DZ", "PL", "GR", "GS", "UP", "SG", "IC")
        private val FROZEN_CODES = listOf("SN", "SG", "PL", "IC")
        private val CLEAR_COVERS = setOf("CLR", "SKC", "NSC", "FEW")
        private val OVERCAST_COVERS = setOf("BKN", "OVC", "VV")
    }
}

internal data class BenchmarkStation(
    val id: String,
    val location: Coordinate,
    val elevation: Distance,
    val description: String,
    val observations: List<BenchmarkObservation>
) {
    companion object {
        private const val RESOURCE_ROOT = "/weather_benchmark"

        fun loadAll(): List<BenchmarkStation> {
            return readResource("stations.csv").drop(1).filter { it.isNotBlank() }.map { line ->
                val (id, latitude, longitude, elevation, description) = line.split(",", limit = 5)
                BenchmarkStation(
                    id,
                    Coordinate(latitude.toDouble(), longitude.toDouble()),
                    Distance.meters(elevation.toFloat()),
                    description.trim('"'),
                    loadObservations(id)
                )
            }
        }

        private fun loadObservations(id: String): List<BenchmarkObservation> {
            val lines = readResource("$id.csv.gz").filter { it.isNotBlank() }
            val columns = lines.first().split(",").withIndex().associate { it.value to it.index }
            return lines.drop(1).map { line ->
                val parts = line.split(",")
                fun column(name: String) = parts[columns.getValue(name)]
                BenchmarkObservation(
                    Instant.parse(column("time")),
                    column("pressure_hpa").toFloatOrNull(),
                    column("temperature_c").toFloatOrNull(),
                    column("humidity_percent").toFloatOrNull(),
                    column("precipitation_mm").toFloatOrNull(),
                    column("weather").split(" ").filter { it.isNotBlank() },
                    column("gust_kt").toFloatOrNull(),
                    column("wind_kt").toFloatOrNull(),
                    column("sky_cover").ifBlank { null }
                )
            }.sortedBy { it.time }
        }

        fun loadClimateNormals(): Map<String, Map<Month, Range<Temperature>>> {
            return readResource("climate_normals.csv").drop(1).filter { it.isNotBlank() }
                .map { it.split(",") }
                .groupBy { it[0] }
                .mapValues { (_, rows) ->
                    rows.associate {
                        Month.of(it[1].toInt()) to Range(
                            Temperature.celsius(it[2].toFloat()),
                            Temperature.celsius(it[3].toFloat())
                        )
                    }
                }
        }

        fun loadMonthlyPrecipitation(): Map<String, Map<Month, Distance>> {
            return readResource("climate_normals.csv").drop(1).filter { it.isNotBlank() }
                .map { it.split(",") }
                .groupBy { it[0] }
                .mapValues { (_, rows) ->
                    rows.associate { Month.of(it[1].toInt()) to Distance.from(it[4].toFloat(), DistanceUnits.Millimeters) }
                }
        }

        private fun readResource(name: String): List<String> {
            val stream = BenchmarkStation::class.java.getResourceAsStream("$RESOURCE_ROOT/$name")
                ?: error("Missing benchmark resource $name, run scripts/download_weather_benchmark.py")
            val text = if (name.endsWith(".gz")) GZIPInputStream(stream) else stream
            return text.bufferedReader().use { it.readLines() }
        }
    }
}
