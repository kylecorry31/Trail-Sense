package com.kylecorry.trail_sense.tools.climate

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.science.ecology.Ecology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GrowingDegreeDaysThresholdTest {

    private data class ThresholdSample(
        val date: LocalDate,
        val coordinate: Coordinate
    )

    private lateinit var weather: WeatherSubsystem

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppServiceRegistry.register(FileSubsystem.getInstance(context))
        weather = WeatherSubsystem.getInstance(context)
    }

//    @Test
    fun calculateGddThreshold() = runBlocking {
        val baseTemperature = Temperature.celsius(10f)

        val samples = listOf(
            ThresholdSample(
                date = LocalDate.of(2026, 4, 30),
                coordinate = Coordinate(41.825226, -71.418884)
            ),
            ThresholdSample(
                date = LocalDate.of(2026, 5, 15),
                coordinate = Coordinate(43.207359, -71.551247)
            ),
            ThresholdSample(
                date = LocalDate.of(2026, 6, 15),
                coordinate = Coordinate(39.742043, -104.991531)
            )
        )

        val totals = samples.map { sample ->
            val elevation = Distance.meters(DEM.getElevation(sample.coordinate))
            val temperatures = weather.getTemperatureRanges(
                sample.date.year,
                sample.coordinate,
                elevation,
                calibrated = false
            )

            val dates = temperatures.map { it.first }.filter { !it.isAfter(sample.date) }
            val cumulative = Ecology.getCumulativeGrowingDegreeDays(
                dates = dates,
                baseTemperature = baseTemperature,
                temperatureProvider = { date ->
                    val range = temperatures.firstOrNull { it.first == date }?.second
                        ?: temperatures.firstOrNull {
                            it.first.month == date.month && it.first.dayOfMonth == date.dayOfMonth
                        }?.second
                    requireNotNull(range) { "Missing temperature for $date" }
                }
            )
            val total = cumulative.lastOrNull()?.second ?: 0f

            val label = "${sample.coordinate.latitude}, ${sample.coordinate.longitude}"
            println("$label (${sample.date}): $total")
            total
        }

        val average = if (totals.isEmpty()) 0f else totals.sum() / totals.size
        println("Average GDD threshold: $average")
    }
}
