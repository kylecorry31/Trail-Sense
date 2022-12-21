package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.trail_sense.Locations
import com.kylecorry.trail_sense.Temperatures
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.math.abs

internal class WeatherSubsystemTest {

    @Test
    fun testYearlyTemperatureRanges() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        val locations = listOf(
            Locations.canada,
            Locations.alaska,
            Locations.california,
            Locations.mexico,
            Locations.costaRica,
            Locations.puertoRico,
            Locations.newYork,
            Locations.greenland,
            Locations.hawaii,
            Locations.equador,
            Locations.brazil,
            Locations.argentina,
            Locations.bolivia,
            Locations.chile,
            Locations.morocco,
            Locations.egypt,
            Locations.chad,
            Locations.southAfrica,
            Locations.madagascar,
            Locations.france,
            Locations.unitedKingdom,
            Locations.ukraine,
            Locations.sweden,
            Locations.siberia,
            Locations.mongolia,
            Locations.moscow,
            Locations.saudiArabia,
            Locations.india,
            Locations.china,
            Locations.indonesia,
            Locations.westernAustralia,
            Locations.queensland,
            Locations.southAustralia,
            Locations.newSouthWales,
        )
        val elevations = listOf(
            Locations.canadaElevation,
            Locations.alaskaElevation,
            Locations.californiaElevation,
            Locations.mexicoElevation,
            Locations.costaRicaElevation,
            Locations.puertoRicoElevation,
            Locations.newYorkElevation,
            Locations.greenlandElevation,
            Locations.hawaiiElevation,
            Locations.equadorElevation,
            Locations.brazilElevation,
            Locations.argentinaElevation,
            Locations.boliviaElevation,
            Locations.chileElevation,
            Locations.moroccoElevation,
            Locations.egyptElevation,
            Locations.chadElevation,
            Locations.southAfricaElevation,
            Locations.madagascarElevation,
            Locations.franceElevation,
            Locations.unitedKingdomElevation,
            Locations.ukraineElevation,
            Locations.swedenElevation,
            Locations.siberiaElevation,
            Locations.mongoliaElevation,
            Locations.moscowElevation,
            Locations.saudiArabiaElevation,
            Locations.indiaElevation,
            Locations.chinaElevation,
            Locations.indonesiaElevation,
            Locations.westernAustraliaElevation,
            Locations.queenslandElevation,
            Locations.southAustraliaElevation,
            Locations.newSouthWalesElevation,
        )
        val lows = listOf(
            Temperatures.canadaLow,
            Temperatures.alaskaLow,
            Temperatures.californiaLow,
            Temperatures.mexicoLow,
            Temperatures.costaRicaLow,
            Temperatures.puertoRicoLow,
            Temperatures.newYorkLow,
            Temperatures.greenlandLow,
            Temperatures.hawaiiLow,
            Temperatures.equadorLow,
            Temperatures.brazilLow,
            Temperatures.argentinaLow,
            Temperatures.boliviaLow,
            Temperatures.chileLow,
            Temperatures.moroccoLow,
            Temperatures.egyptLow,
            Temperatures.chadLow,
            Temperatures.southAfricaLow,
            Temperatures.madagascarLow,
            Temperatures.franceLow,
            Temperatures.unitedKingdomLow,
            Temperatures.ukraineLow,
            Temperatures.swedenLow,
            Temperatures.siberiaLow,
            Temperatures.mongoliaLow,
            Temperatures.moscowLow,
            Temperatures.saudiArabiaLow,
            Temperatures.indiaLow,
            Temperatures.chinaLow,
            Temperatures.indonesiaLow,
            Temperatures.westernAustraliaLow,
            Temperatures.queenslandLow,
            Temperatures.southAustraliaLow,
            Temperatures.newSouthWalesLow,
        )
        val highs = listOf(
            Temperatures.canadaHigh,
            Temperatures.alaskaHigh,
            Temperatures.californiaHigh,
            Temperatures.mexicoHigh,
            Temperatures.costaRicaHigh,
            Temperatures.puertoRicoHigh,
            Temperatures.newYorkHigh,
            Temperatures.greenlandHigh,
            Temperatures.hawaiiHigh,
            Temperatures.equadorHigh,
            Temperatures.brazilHigh,
            Temperatures.argentinaHigh,
            Temperatures.boliviaHigh,
            Temperatures.chileHigh,
            Temperatures.moroccoHigh,
            Temperatures.egyptHigh,
            Temperatures.chadHigh,
            Temperatures.southAfricaHigh,
            Temperatures.madagascarHigh,
            Temperatures.franceHigh,
            Temperatures.unitedKingdomHigh,
            Temperatures.ukraineHigh,
            Temperatures.swedenHigh,
            Temperatures.siberiaHigh,
            Temperatures.mongoliaHigh,
            Temperatures.moscowHigh,
            Temperatures.saudiArabiaHigh,
            Temperatures.indiaHigh,
            Temperatures.chinaHigh,
            Temperatures.indonesiaHigh,
            Temperatures.westernAustraliaHigh,
            Temperatures.queenslandHigh,
            Temperatures.southAustraliaHigh,
            Temperatures.newSouthWalesHigh,
        )

        val maxTempDiff = 8f
        val maxTempRangeDiff = 5f
        val maxAverageDelta = 1.5f

        var total = 0
        var deltas = 0f
        for (i in locations.indices) {
            val actual = subsystem.getTemperatureRanges(2022, locations[i], elevations[i], false)
                .filter { it.first.dayOfMonth == 15 }
                .map { it.second.start.temperature to it.second.end.temperature }

            val actualLows = actual.map { it.first }
            val actualHighs = actual.map { it.second }

            actualLows.forEachIndexed { index, value ->
                deltas += abs(lows[i][index] - value)
                total++
                Assert.assertEquals(lows[i][index], value, maxTempDiff)
            }

            actualHighs.forEachIndexed { index, value ->
                deltas += abs(highs[i][index] - value)
                total++
                Assert.assertEquals(highs[i][index], value, maxTempDiff)
            }

            val actualRanges = actual.map { it.second - it.first }
            val expectedRanges = highs[i].mapIndexed { index, high -> high - lows[i][index] }

            actualRanges.forEachIndexed { index, value ->
                Assert.assertEquals(expectedRanges[index], value, maxTempRangeDiff)
            }
        }

        println(deltas / total)
        Assert.assertTrue((deltas / total) <= maxAverageDelta)

    }

}