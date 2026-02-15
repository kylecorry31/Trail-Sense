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
import java.time.LocalDate
import java.time.Month
import kotlin.math.roundToInt

class GrowingDegreeDaysThresholdTest {

    private lateinit var weather: WeatherSubsystem

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppServiceRegistry.register(FileSubsystem.getInstance(context))
        weather = WeatherSubsystem.getInstance(context)
    }

    //    @Test
    fun getCumulativeGDDChart() = runBlocking {
        // RI
        val location = Coordinate(41.890833, -71.690556)
        val year = 2026
        val baseTemperature = Temperature.celsius(10f)

        val early = 5
        val mid = 15
        val late = 25

        val elevation = Distance.meters(DEM.getElevation(location))
        val temperatures = weather.getTemperatureRanges(
            year,
            location,
            elevation,
            calibrated = false
        )

        val dates = Month.entries.flatMap {
            listOf(
                LocalDate.of(year, it, early),
                LocalDate.of(year, it, mid),
                LocalDate.of(year, it, late),
            )
        }
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

        Month.entries.forEach { month ->
            println(month.name)
            val values = cumulative.filter { it.first.month == month }
            println("Early: ${values[0].second.roundToInt()}\tMid: ${values[1].second.roundToInt()}\tLate: ${values[2].second.roundToInt()}")
            println()
        }
    }
}
