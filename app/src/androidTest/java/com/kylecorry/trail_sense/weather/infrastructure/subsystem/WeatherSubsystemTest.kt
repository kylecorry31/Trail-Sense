package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.trail_sense.Locations
import com.kylecorry.trail_sense.Temperatures
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

internal class WeatherSubsystemTest {

    @Test
    fun testYearlyTemperatureRanges() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        val locations = listOf(
            Locations.canada,
            Locations.alaska,
//            Locations.california,
            Locations.mexico,
            Locations.costaRica,
            Locations.puertoRico,
//            Locations.newYork,
//            Locations.greenland,
//            Locations.hawaii
        )
        val elevations = listOf(
            Locations.canadaElevation,
            Locations.alaskaElevation,
//            Locations.californiaElevation,
            Locations.mexicoElevation,
            Locations.costaRicaElevation,
            Locations.puertoRicoElevation,
//            Locations.newYorkElevation,
//            Locations.greenlandElevation,
//            Locations.hawaiiElevation
        )
        val lows = listOf(
            Temperatures.canadaLow,
            Temperatures.alaskaLow,
//            Temperatures.californiaLow,
            Temperatures.mexicoLow,
            Temperatures.costaRicaLow,
            Temperatures.puertoRicoLow,
//            Temperatures.newYorkLow,
//            Temperatures.greenlandLow,
//            Temperatures.hawaiiLow
        )
        val highs = listOf(
            Temperatures.canadaHigh,
            Temperatures.alaskaHigh,
//            Temperatures.californiaHigh,
            Temperatures.mexicoHigh,
            Temperatures.costaRicaHigh,
            Temperatures.puertoRicoHigh,
//            Temperatures.newYorkHigh,
//            Temperatures.greenlandHigh,
//            Temperatures.hawaiiHigh
        )

        for (i in locations.indices){
            val actual = subsystem.getTemperatureRanges(2022, locations[i], elevations[i])
                .filter { it.first.dayOfMonth == 15 }
                .map { it.second.start.temperature to it.second.end.temperature }

            val actualLows = actual.map { it.first }
            val actualHighs = actual.map { it.second }

            actualLows.forEachIndexed { index, value ->
                Assert.assertEquals(lows[i][index], value, 3f)
            }

            actualHighs.forEachIndexed { index, value ->
                Assert.assertEquals(highs[i][index], value, 4f)
            }
        }

    }

}