package com.kylecorry.trail_sense.tools.weather.benchmark

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricTemperatureRepo
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.PressureForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.LocalClimate
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.WeatherForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.HistoricTemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.sealevel.LoessSeaLevelCalibrationStrategy
import com.kylecorry.trail_sense.tools.weather.domain.sealevel.NullSeaLevelCalibrationStrategy
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.Month
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.random.Random

@Tag("manual")
internal class WeatherForecastBenchmarkTest {

    @Test
    fun benchmark() = runBlocking {
        val stations = BenchmarkStation.loadAll()
        val climateNormals = BenchmarkStation.loadClimateNormals()
        val monthlyPrecipitation = BenchmarkStation.loadMonthlyPrecipitation()
        val scores = stations.parallelMap {
            score(
                it,
                climateNormals.getValue(it.id),
                monthlyPrecipitation = monthlyPrecipitation.getValue(it.id)
            )
        }
        val stationResults = scores.associate { it.station to it.metrics() }
        val results = stationResults + (ALL to scores.reduce { a, b -> a + b }.metrics() + getStationMeans(stationResults))

        val outputDirectory = File("build/reports/weather-benchmark").apply { mkdirs() }
        File(outputDirectory, "results.csv").writeText(toCsv(results))
        File(outputDirectory, "report.md").writeText(toReport(stations, results))
    }

    @Test
    fun shortHistory() = runBlocking {
        val stations = BenchmarkStation.loadAll()
        val climateNormals = BenchmarkStation.loadClimateNormals()
        val monthlyPrecipitation = BenchmarkStation.loadMonthlyPrecipitation()
        val builder = StringBuilder("# Weather forecast benchmark with short history\n\n")
        builder.append("Each forecast only sees the given hours of hourly readings. HSS: 1 is perfect, 0 is chance.\n\n")
        builder.append("| History | Projection MAE +3 h | No change MAE +3 h | " + SHORT_HISTORY_CONDITIONS.joinToString(" | ") + " |\n")
        builder.append("|---" + "|---".repeat(SHORT_HISTORY_CONDITIONS.size + 2) + "|\n")
        val scenarios = listOf(3L, 6L, 12L, 48L).map { "$it h" to (Duration.ofHours(it) to 0f) } +
            listOf(6L, 48L).map { "$it h, 40% missing" to (Duration.ofHours(it) to MISSING_FRACTION) }
        for ((label, scenario) in scenarios) {
            val (hours, missing) = scenario
            val metrics = stations.parallelMap {
                score(it, climateNormals.getValue(it.id), monthlyPrecipitation.getValue(it.id), hours, missing)
            }
                .reduce { a, b -> a + b }.metrics()
            builder.append(
                "| $label | ${"%.2f".format(metrics.getValue("projection_mae_3h"))} | ${"%.2f".format(metrics.getValue("no_change_mae_3h"))} | " +
                    SHORT_HISTORY_CONDITIONS.joinToString(" | ") { "%.3f".format(metrics.getValue("${it}_hss")) } + " |\n"
            )
        }
        val outputDirectory = File("build/reports/weather-benchmark").apply { mkdirs() }
        File(outputDirectory, "short-history.md").writeText(builder.toString())
    }

    private suspend fun score(
        station: BenchmarkStation,
        climateNormals: Map<Month, Range<Temperature>>,
        monthlyPrecipitation: Map<Month, Distance>,
        maxHistory: Duration = HISTORY,
        missingFraction: Float = 0f
    ): StationScore {
        val score = StationScore(station.id)
        val observations = station.observations
        val times = observations.map { it.time }
        val random = Random(station.id.hashCode())
        val withPressure = observations.filter { it.pressureHpa != null && random.nextFloat() >= missingFraction }
        val pressureByHour = observations.filter { it.pressureHpa != null }.associate { hourKey(it.time) to it.pressureHpa!! }
        val start = withPressure.first().time.plus(WARM_UP)
        val end = observations.last().time.minus(LATER_WINDOW_END)
        val temperatures = HistoricTemperatureService(
            HistoricTemperatureRepo { climateNormals },
            station.location,
            station.elevation
        )
        val prefs = defaultPreferences()
        var historyStart = 0
        var lastForecastHour: Long? = null

        for ((index, current) in withPressure.withIndex()) {
            val now = current.time
            while (Duration.between(withPressure[historyStart].time, now) > maxHistory) {
                historyStart++
            }
            // WHY: Forecasts minutes apart are nearly identical, so one per hour scores the same and runs much faster
            if (now < start || now > end || hourKey(now) == lastForecastHour) {
                continue
            }
            lastForecastHour = hourKey(now)
            val history = toWeatherObservations(station, withPressure.subList(historyStart, index + 1))
            val clock = Clock.fixed(now, ZoneOffset.UTC)
            val next = observations.subList(times.firstIndexAtOrAfter(now), times.firstIndexAfter(now.plus(EVENT_WINDOW)))
            val nextSky = observations.subList(times.firstIndexAtOrAfter(now), times.firstIndexAfter(now.plus(SKY_WINDOW)))
                .filter { it.skyCover != null }

            val climate = LocalClimate(
                monthlyPrecipitation,
                temperatures.getTemperatureRange(now.atZone(ZoneOffset.UTC).toLocalDate())
            )
            val weather = WeatherForecaster(temperatures, prefs, clock, climate)
                .forecast(history, emptyList(), station.location)
            val conditions = weather.prediction.hourly.map { it.condition }
            score.precipitation.add(WeatherCondition.Precipitation in conditions, next.any { it.isWet })
            score.storm.add(WeatherCondition.Storm in conditions, next.any { it.isStormy })
            score.thunderstorm.add(WeatherCondition.Thunderstorm in conditions, next.any { it.isThunder })
            score.wind.add(WeatherCondition.Wind in conditions, next.any { it.isWindy })
            if (nextSky.isNotEmpty()) {
                score.clear.add(WeatherCondition.Clear in conditions, nextSky.count { it.isClearSky } * 2 > nextSky.size)
                score.overcast.add(WeatherCondition.Overcast in conditions, nextSky.count { it.isOvercastSky } * 2 > nextSky.size)
            }
            val later = observations.subList(
                times.firstIndexAtOrAfter(now.plus(LATER_WINDOW_START)),
                times.firstIndexAfter(now.plus(LATER_WINDOW_END))
            )
            val laterSky = later.filter { it.skyCover != null }
            val laterConditions = weather.prediction.daily.map { it.condition }
            score.laterPrecipitation.add(WeatherCondition.Precipitation in laterConditions, later.any { it.isWet })
            if (laterSky.isNotEmpty()) {
                score.laterClear.add(WeatherCondition.Clear in laterConditions, laterSky.count { it.isClearSky } * 2 > laterSky.size)
                score.laterOvercast.add(
                    WeatherCondition.Overcast in laterConditions,
                    laterSky.count { it.isOvercastSky } * 2 > laterSky.size
                )
            }
            val firstPrecipitation = next.firstOrNull { it.isWet }
            val predictedType = when {
                WeatherCondition.Snow in conditions -> true
                WeatherCondition.Rain in conditions -> false
                else -> null
            }
            if (firstPrecipitation != null && predictedType != null) {
                score.precipitationType.add(predictedType, firstPrecipitation.isSnow)
            }

            val projection = PressureForecaster().forecast(history, false)
            val currentPressure = history.last().pressure.hpa().value
            for (lead in LEADS) {
                val actual = pressureByHour[hourKey(now.plus(Duration.ofHours(lead)))] ?: continue
                score.noChangeErrors.getValue(lead).add(abs(currentPressure - actual))
                val projected = projection.firstOrNull { abs(Duration.between(now, it.time).toMinutes() - lead * 60) <= 30 }
                if (projected != null) {
                    score.projectionErrors.getValue(lead).add(abs(projected.value.hpa().value - actual))
                }
            }
        }
        return score
    }

    // WHY: Pooled scores reward telling rainy places from dry ones, so the mean of each station's score shows skill at a place
    private fun getStationMeans(stationResults: Map<String, Map<String, Double>>): Map<String, Double> {
        val skillScores = stationResults.values.flatMap { it.keys }.filter { it.endsWith("_hss") }.distinct()
        return skillScores.associate { metric ->
            "station_mean_$metric" to stationResults.values.mapNotNull { it[metric] }.filterNot { it.isNaN() }.average()
        }
    }

    private fun toCoverage(stations: List<BenchmarkStation>): String {
        val builder = StringBuilder("## Data coverage\n\n")
        builder.append("Hours with each condition observed, the sea level pressure range, and the fastest 3 hour fall.\n\n")
        builder.append("| Station | Dates | Rain | Snow | Thunder | Wind | Clear | Overcast | Pressure (hPa) | Fastest 3 h fall |\n")
        builder.append("|---|---|---|---|---|---|---|---|---|---|\n")
        for (station in stations) {
            val observations = station.observations
            val hourly = observations.groupBy { hourKey(it.time) }
            fun hours(predicate: (BenchmarkObservation) -> Boolean) = hourly.values.count { hour -> hour.any(predicate) }
            val pressures = observations.filter { it.pressureHpa != null }
            val byHour = pressures.associate { hourKey(it.time) to it.pressureHpa!! }
            val fastestFall = byHour.mapNotNull { (hour, pressure) -> byHour[hour - 3]?.let { pressure - it } }.minOrNull() ?: 0f
            builder.append(
                "| ${station.id} | ${observations.first().time.toString().take(10)} to ${observations.last().time.toString().take(10)} " +
                    "| ${hours { it.isWet && !it.isSnow }} | ${hours { it.isSnow }} | ${hours { it.isThunder }} | ${hours { it.isWindy }} " +
                    "| ${hours { it.isClearSky }} | ${hours { it.isOvercastSky }} " +
                    "| ${"%.0f".format(pressures.minOf { it.pressureHpa!! })} to ${"%.0f".format(pressures.maxOf { it.pressureHpa!! })} " +
                    "| ${"%.1f".format(fastestFall)} |\n"
            )
        }
        return builder.append("\n").toString()
    }

    private suspend fun <T, R> List<T>.parallelMap(transform: suspend (T) -> R): List<R> = coroutineScope {
        map { async(Dispatchers.Default) { transform(it) } }.awaitAll()
    }

    private fun List<Instant>.firstIndexAtOrAfter(time: Instant): Int {
        val index = binarySearch(time)
        return if (index >= 0) index else -index - 1
    }

    private fun List<Instant>.firstIndexAfter(time: Instant): Int {
        var index = firstIndexAtOrAfter(time)
        while (index < size && this[index] == time) {
            index++
        }
        return index
    }

    private fun toWeatherObservations(
        station: BenchmarkStation,
        recent: List<BenchmarkObservation>
    ): List<WeatherObservation> {
        val raw = recent.mapIndexed { i, observation ->
            Reading(
                RawWeatherObservation(
                    i.toLong(),
                    observation.pressureHpa!!,
                    0f,
                    observation.temperatureC ?: 0f,
                    location = station.location
                ),
                observation.time
            )
        }
        val smoothed = LoessSeaLevelCalibrationStrategy(
            NullSeaLevelCalibrationStrategy(),
            LoessSeaLevelCalibrationStrategy.DEFAULT_SMOOTHING
        ).calibrate(raw)
        return smoothed.zip(raw) { pressure, reading ->
            WeatherObservation(
                reading.value.id,
                reading.time,
                pressure.value,
                Temperature.celsius(reading.value.temperature),
                null,
                station.location
            )
        }
    }

    private fun defaultPreferences(): IWeatherPreferences {
        val prefs = mock<IWeatherPreferences>()
        whenever(prefs.forecastSensitivity).thenReturn(ForecastSensitivity.Medium)
        whenever(prefs.stormSensitivity).thenReturn(ForecastSensitivity.Medium)
        return prefs
    }

    private fun toCsv(results: Map<String, Map<String, Double>>): String {
        val lines = results.flatMap { (station, metrics) ->
            metrics.map { (metric, value) -> "$station,$metric,${"%.4f".format(value)}" }
        }
        return (listOf("station,metric,value") + lines).joinToString("\n") + "\n"
    }

    private fun toReport(
        stations: List<BenchmarkStation>,
        results: Map<String, Map<String, Double>>
    ): String {
        val builder = StringBuilder("# Weather forecast benchmark\n\n")
        builder.append("MAE is hPa (lower is better). POD, CSI and HSS are higher is better; FAR is lower is better.\n\n")
        builder.append(toCoverage(stations))
        for (station in listOf(ALL) + stations.map { it.id }) {
            val description = stations.firstOrNull { it.id == station }?.description ?: "All stations"
            builder.append("## $station: $description\n\n| Metric | Value |\n|---|---|\n")
            for ((metric, value) in results.getValue(station)) {
                builder.append("| $metric | ${"%.3f".format(value)} |\n")
            }
            builder.append("\n")
        }
        return builder.toString()
    }

    private fun hourKey(time: Instant): Long {
        return (time.epochSecond + SECONDS_PER_HOUR / 2) / SECONDS_PER_HOUR
    }

    private class Contingency(var hits: Int = 0, var falseAlarms: Int = 0, var misses: Int = 0, var correctNegatives: Int = 0) {
        fun add(predicted: Boolean, observed: Boolean) {
            when {
                predicted && observed -> hits++
                predicted -> falseAlarms++
                observed -> misses++
                else -> correctNegatives++
            }
        }

        operator fun plus(other: Contingency): Contingency {
            return Contingency(
                hits + other.hits,
                falseAlarms + other.falseAlarms,
                misses + other.misses,
                correctNegatives + other.correctNegatives
            )
        }

        fun metrics(prefix: String): Map<String, Double> {
            val total = (hits + falseAlarms + misses + correctNegatives).toDouble()
            val expected = ((hits + misses).toDouble() * (hits + falseAlarms) +
                (misses + correctNegatives).toDouble() * (falseAlarms + correctNegatives)) / total
            return mapOf(
                "${prefix}_pod" to ratio(hits, hits + misses),
                "${prefix}_far" to ratio(falseAlarms, hits + falseAlarms),
                "${prefix}_csi" to ratio(hits, hits + misses + falseAlarms),
                "${prefix}_hss" to if (total == expected) 0.0 else (hits + correctNegatives - expected) / (total - expected),
                "${prefix}_predicted_rate" to (hits + falseAlarms) / total,
                "${prefix}_observed_rate" to (hits + misses) / total
            )
        }

        val total: Int
            get() = hits + falseAlarms + misses + correctNegatives

        fun accuracy(): Double {
            return ratio(hits + correctNegatives, total)
        }

        private fun ratio(numerator: Int, denominator: Int): Double {
            return if (denominator == 0) 0.0 else numerator.toDouble() / denominator
        }
    }

    private class StationScore(val station: String) {
        val projectionErrors = LEADS.associateWith { mutableListOf<Float>() }
        val noChangeErrors = LEADS.associateWith { mutableListOf<Float>() }
        var precipitation = Contingency()
        var storm = Contingency()
        var thunderstorm = Contingency()
        var wind = Contingency()
        var clear = Contingency()
        var overcast = Contingency()
        var precipitationType = Contingency()
        var laterPrecipitation = Contingency()
        var laterClear = Contingency()
        var laterOvercast = Contingency()

        operator fun plus(other: StationScore): StationScore {
            val combined = StationScore(ALL)
            for (lead in LEADS) {
                combined.projectionErrors.getValue(lead).addAll(projectionErrors.getValue(lead) + other.projectionErrors.getValue(lead))
                combined.noChangeErrors.getValue(lead).addAll(noChangeErrors.getValue(lead) + other.noChangeErrors.getValue(lead))
            }
            combined.precipitation = precipitation + other.precipitation
            combined.storm = storm + other.storm
            combined.thunderstorm = thunderstorm + other.thunderstorm
            combined.wind = wind + other.wind
            combined.clear = clear + other.clear
            combined.overcast = overcast + other.overcast
            combined.precipitationType = precipitationType + other.precipitationType
            combined.laterPrecipitation = laterPrecipitation + other.laterPrecipitation
            combined.laterClear = laterClear + other.laterClear
            combined.laterOvercast = laterOvercast + other.laterOvercast
            return combined
        }

        fun metrics(): Map<String, Double> {
            val projection = LEADS.associate { "projection_mae_${it}h" to projectionErrors.getValue(it).average() }
            val noChange = LEADS.associate { "no_change_mae_${it}h" to noChangeErrors.getValue(it).average() }
            val coverage = LEADS.associate {
                "projection_coverage_${it}h" to projectionErrors.getValue(it).size.toDouble() /
                    noChangeErrors.getValue(it).size.coerceAtLeast(1)
            }
            return projection + noChange + coverage +
                precipitation.metrics("precipitation") +
                storm.metrics("storm") +
                thunderstorm.metrics("thunderstorm") +
                wind.metrics("wind") +
                clear.metrics("clear") +
                overcast.metrics("overcast") +
                laterPrecipitation.metrics("later_precipitation") +
                laterClear.metrics("later_clear") +
                laterOvercast.metrics("later_overcast") +
                mapOf(
                    "snow_vs_rain_accuracy" to precipitationType.accuracy(),
                    "snow_vs_rain_cases" to precipitationType.total.toDouble()
                )
        }
    }

    companion object {
        private const val ALL = "ALL"
        private const val SECONDS_PER_HOUR = 3600L
        private val LEADS = (1L..5L).toList()
        private val WARM_UP = Duration.ofHours(24)
        private val HISTORY = Duration.ofHours(48)
        private val EVENT_WINDOW = Duration.ofHours(12)
        private val SKY_WINDOW = Duration.ofHours(6)
        private val LATER_WINDOW_START = Duration.ofHours(12)
        private val LATER_WINDOW_END = Duration.ofHours(24)
        private const val MISSING_FRACTION = 0.4f
        private val SHORT_HISTORY_CONDITIONS = listOf(
            "precipitation",
            "storm",
            "thunderstorm",
            "wind",
            "clear",
            "overcast",
            "later_precipitation",
            "later_clear",
            "later_overcast"
        )
    }
}
