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
//            Locations.california, // Not accurate
            Locations.mexico,
            Locations.costaRica,
            Locations.puertoRico,
            Locations.newYork, // Not accurate
//            Locations.greenland, // Not accurate
//            Locations.hawaii, // Not accurate
            Locations.equador, // Not accurate
            Locations.brazil,
            Locations.argentina,
            Locations.bolivia,
            Locations.chile,
        )
        val elevations = listOf(
            Locations.canadaElevation,
            Locations.alaskaElevation,
//            Locations.californiaElevation, // Not accurate
            Locations.mexicoElevation,
            Locations.costaRicaElevation,
            Locations.puertoRicoElevation,
            Locations.newYorkElevation, // Not accurate
//            Locations.greenlandElevation, // Not accurate
//            Locations.hawaiiElevation, // Not accurate
            Locations.equadorElevation, // Not accurate
            Locations.brazilElevation,
            Locations.argentinaElevation,
            Locations.boliviaElevation,
            Locations.chileElevation,
        )
        val lows = listOf(
            Temperatures.canadaLow,
            Temperatures.alaskaLow,
//            Temperatures.californiaLow, // Not accurate
            Temperatures.mexicoLow,
            Temperatures.costaRicaLow,
            Temperatures.puertoRicoLow,
            Temperatures.newYorkLow, // Not accurate
//            Temperatures.greenlandLow, // Not accurate
//            Temperatures.hawaiiLow, // Not accurate
            Temperatures.equadorLow, // Not accurate
            Temperatures.brazilLow,
            Temperatures.argentinaLow,
            Temperatures.boliviaLow,
            Temperatures.chileLow,
        )
        val highs = listOf(
            Temperatures.canadaHigh,
            Temperatures.alaskaHigh,
//            Temperatures.californiaHigh, // Not accurate
            Temperatures.mexicoHigh,
            Temperatures.costaRicaHigh,
            Temperatures.puertoRicoHigh,
            Temperatures.newYorkHigh, // Not accurate
//            Temperatures.greenlandHigh, // Not accurate
//            Temperatures.hawaiiHigh, // Not accurate
            Temperatures.equadorHigh, // Not accurate
            Temperatures.brazilHigh,
            Temperatures.argentinaHigh,
            Temperatures.boliviaHigh,
            Temperatures.chileHigh,
        )

        // TODO: Get this delta down to 5
        val maxTempDiff = 10f
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