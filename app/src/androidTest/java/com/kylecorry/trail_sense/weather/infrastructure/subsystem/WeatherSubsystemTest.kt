package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Location
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertBias
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertWithin
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricMonthlyTemperatureRangeRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Month

internal class WeatherSubsystemTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(context))
    }

    @Test
    fun climateZone() = runBlocking {
        // Source of truth: https://koppen.earth/
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        val places = listOf(
            location(-4.518814742813397, -64.54611891693324) to "Af",
            location(-6.221323527569555, -57.75469757904136) to "Am",
            location(-12.755153459727236, -60.129570938783964) to "As",
            location(26.427732529097433, 11.642537825154815) to "BWh",
            location(37.11, 79.93) to "BWk",
            location(10.889781243153859, 17.840790318309242) to "BSh",
            location(-31.5, 24.5) to "BSk",
            location(34.760618265160815, -5.4566297837873625) to "Csa",
            location(45.52684910275576, -122.77664603487463) to "Csb",
            location(25.440252914660682, 83.93974525268311) to "Cwa",
            location(19.94868082137199, -100.17999896176053) to "Cwb",
            location(35.768295580708255, -81.4029610545656) to "Cfa",
            location(47.61887607873789, 2.3071200209801765) to "Cfb",
            location(57.32387528320942, -4.077455914853912) to "Cfc",
            location(38.76062888403598, 41.51727954258531) to "Dsa",
            location(38.74488121796104, 36.92826402609207) to "Dsb",
            location(42.4474624964996, 122.11758394841614) to "Dwa",
            location(49.097957757816225, 127.3216568354164) to "Dwb",
            location(54.35765122332575, 126.62354956462677) to "Dwc",
            location(62.5, 135.5) to "Dwd",
            location(42, -72, 0f) to "Dfa",
            location(45.38076187098529, -69.35700638127085) to "Dfb",
            location(54.770550236146356, -74.47190511134566) to "Dfc",
            location(73.06625156704135, -121.51469117406504) to "ET",
            location(-80.55071870183689, -91.46659018836088) to "EF"
        )


        for (place in places) {
            val classification = subsystem.getClimateClassification(
                place.first.coordinate,
                place.first.elevation,
                false
            )
            assertEquals(
                "Expected ${place.second} for ${place.first.coordinate} but got ${classification.code}",
                place.second,
                classification.code
            )
        }
    }

    @Test
    fun monthlyTemperatureModel() = runBlocking {
        // Reference: the full resolution MERRA-2 sea level monthly highs and lows (°C) at 100 points in hiking areas
        // and across the land of the world, from generate-temperature-test-data.py in the Earth Model project, so
        // this shows the error added by shrinking the model.
        val errors = mutableListOf<Float>()
        for (point in loadTemperaturePoints()) {
            val actual = HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(point.location)
            Month.entries.forEachIndexed { i, month ->
                val range = actual[month]!!
                errors.add(range.end.celsius().value - point.highs[i])
                errors.add(range.start.celsius().value - point.lows[i])
            }
        }

        assertQuantile(errors, 0.3f, 0.5f, "Temperature")
        assertQuantile(errors, 0.6f, 0.9f, "Temperature")
        assertQuantile(errors, 1f, 0.99f, "Temperature")
        assertWithin(errors, 1.5f, 1f, "Temperature")
        assertBias(errors, 0.1f, "Temperature")
    }

    private fun loadTemperaturePoints(): List<TemperaturePoint> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return assets.open("temperature_tests/points.csv").bufferedReader().readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val row = line.split(",")
                val values = row.drop(2).map { it.toFloat() }
                TemperaturePoint(
                    Coordinate(row[0].toDouble(), row[1].toDouble()),
                    values.take(12),
                    values.drop(12)
                )
            }
    }

    @Test
    fun precipitation() = runBlocking {
        // Reference: the full resolution ERA5 precipitation (in mm) at 100 points in hiking areas and across the land
        // of the world, from generate-precipitation-test-data.py in the Earth Model project, so this shows the error
        // added by shrinking the model.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        // Errors in mm for each month
        val errors = mutableListOf<Float>()
        for (point in loadPrecipitationPoints()) {
            val actual = subsystem.getMonthlyPrecipitation(point.location)
            Month.entries.forEachIndexed { i, month ->
                val predicted = actual[month]!!.convertTo(DistanceUnits.Millimeters).value
                errors.add(predicted - point.months[i])
            }
        }

        assertQuantile(errors, 6f, 0.5f, "Precipitation")
        assertQuantile(errors, 25f, 0.9f, "Precipitation")
        assertQuantile(errors, 70f, 0.99f, "Precipitation")
        assertWithin(errors, 25f, 0.9f, "Precipitation")
        assertBias(errors, 1f, "Precipitation")
    }

    private fun loadPrecipitationPoints(): List<PrecipitationPoint> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return assets.open("precipitation_tests/points.csv").bufferedReader().readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val row = line.split(",")
                PrecipitationPoint(
                    Coordinate(row[0].toDouble(), row[1].toDouble()),
                    row.drop(2).map { it.toFloat() }
                )
            }
    }

    private fun location(latitude: Number, longitude: Number, elevation: Number? = null): Location =
        runBlocking {
            val ele = elevation?.toFloat() ?: DEM.getElevation(
                Coordinate(
                    latitude.toDouble(),
                    longitude.toDouble()
                )
                ).elevation
            Location(
                Coordinate(latitude.toDouble(), longitude.toDouble()),
                Distance.meters(ele)
            )
        }

    private class TemperaturePoint(
        val location: Coordinate,
        val highs: List<Float>,
        val lows: List<Float>
    )

    private class PrecipitationPoint(
        val location: Coordinate,
        val months: List<Float>
    )


}
