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
//            Locations.hawaii,
//            Locations.equador,
            Locations.brazil,
            Locations.argentina,
            Locations.bolivia,
            Locations.chile,
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
//            Locations.hawaiiElevation,
//            Locations.equadorElevation,
            Locations.brazilElevation,
            Locations.argentinaElevation,
            Locations.boliviaElevation,
            Locations.chileElevation,
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
//            Temperatures.hawaiiLow,
//            Temperatures.equadorLow,
            Temperatures.brazilLow,
            Temperatures.argentinaLow,
            Temperatures.boliviaLow,
            Temperatures.chileLow,
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
//            Temperatures.hawaiiHigh,
//            Temperatures.equadorHigh,
            Temperatures.brazilHigh,
            Temperatures.argentinaHigh,
            Temperatures.boliviaHigh,
            Temperatures.chileHigh,
        )

        val maxTempDiff = 5f
        val maxTempRangeDiff = 3f

        for (i in locations.indices){
            val actual = subsystem.getTemperatureRanges(2022, locations[i], elevations[i])
                .filter { it.first.dayOfMonth == 15 }
                .map { it.second.start.temperature to it.second.end.temperature }

            val actualLows = actual.map { it.first }
            val actualHighs = actual.map { it.second }

            actualLows.forEachIndexed { index, value ->
                Assert.assertEquals(lows[i][index], value, maxTempDiff)
            }

            actualHighs.forEachIndexed { index, value ->
                Assert.assertEquals(highs[i][index], value, maxTempDiff)
            }

            val actualRanges = actual.map { it.second - it.first }
            val expectedRanges = highs[i].mapIndexed { index, high -> high - lows[i][index] }

            actualRanges.forEachIndexed { index, value ->
                Assert.assertEquals(expectedRanges[index], value, maxTempRangeDiff)
            }
        }

    }

}