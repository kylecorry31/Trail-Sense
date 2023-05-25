package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

internal class WeatherSubsystemTest {

    @Test
    fun temperature() = runBlocking {
        // Source of truth: https://weatherspark.com/
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        val maximumError = 9f
        val maximumAverageError = 3f
        val maximumStdDevError = 3f

        val errors = mutableListOf<Float>()

        val places = listOf(
            Place(
                "New York",
                41.714,
                -74.006,
                30,
                Day(1, 29, 28, 39),
                Day(3, 12, 34, 48),
                Day(6, 2, 61, 76),
                Day(7, 21, 71, 85),
                Day(9, 16, 63, 76),
                Day(12, 3, 37, 48)
            ),
            Place(
                "Orlando",
                28.538,
                -81.379,
                98,
                Day(1, 15, 52, 70),
                Day(2, 25, 55, 74),
                Day(5, 15, 69, 87),
                Day(7, 22, 76, 91),
                Day(9, 28, 73, 87),
                Day(12, 4, 58, 74)
            ),
            Place(
                "Los Angeles",
                34.052,
                -118.244,
                289,
                Day(3, 23, 53, 70),
                Day(7, 1, 64, 81),
                Day(8, 25, 66, 85),
                Day(9, 30, 63, 81),
                Day(12, 25, 48, 67)
            ),
            Place(
                "Quito",
                -0.230,
                -78.525,
                9364,
                Day(2, 2, 49, 64),
                Day(4, 25, 49, 64),
                Day(7, 16, 48, 65),
                Day(9, 20, 48, 66)
            ),
            Place(
                "London",
                51.509,
                -0.126,
                82,
                Day(2, 7, 39, 47),
                Day(3, 18, 42, 53),
                Day(6, 15, 55, 69),
                Day(9, 1, 60, 74),
                Day(9, 7, 56, 69),
                Day(11, 16, 44, 53)
            ),
            Place(
                "Anchorage",
                61.218,
                -149.900,
                98,
                Day(1, 17, 13, 23),
                Day(3, 5, 17, 32),
                Day(7, 20, 55, 68),
                Day(9, 11, 46, 59),
                Day(11, 6, 22, 32)
            ),
            Place(
                "Amesterdam",
                52.374,
                4.890,
                43,
                Day(2, 8, 34, 42),
                Day(3, 13, 37, 48),
                Day(6, 11, 51, 65),
                Day(8, 5, 57, 71),
                Day(9, 13, 52, 65),
                Day(11, 20, 39, 48)
            ),
            Place(
                "Stockholm",
                59.333,
                18.065,
                95,
                Day(2, 8, 22, 32),
                Day(3, 19, 27, 40),
                Day(6, 4, 47, 64),
                Day(7, 22, 56, 72),
                Day(9, 4, 49, 64),
                Day(11, 19, 32, 40)
            ),
            Place(
                "Rio de Janeiro",
                -22.903,
                -43.208,
                16,
                Day(2, 12, 76, 88),
                Day(5, 19, 69, 79),
                Day(7, 22, 65, 77),
                Day(10, 10, 69, 79),
                Day(12, 25, 75, 86)
            ),
            Place(
                "Honolulu",
                21.307,
                -157.858,
                59,
                Day(1, 28, 68, 79),
                Day(4, 1, 70, 81),
                Day(6, 27, 75, 85),
                Day(9, 23, 76, 87),
                Day(10, 14, 75, 85),
                Day(12, 13, 71, 81)
            ),
            Place(
                "Tokyo",
                35.689,
                139.692,
                141,
                Day(1, 26, 36, 48),
                Day(3, 20, 43, 56),
                Day(6, 27, 70, 79),
                Day(9, 9, 77, 87),
                Day(9, 18, 70, 79),
                Day(12, 4, 44, 56)
            ),
            Place(
                "Bangkok",
                13.754,
                100.501,
                39,
                Day(1, 1, 71, 88),
                Day(4, 15, 80, 95),
                Day(10, 12, 78, 89)
            ),
            Place(
                "Sydney",
                -33.868,
                151.207,
                190,
                Day(1, 25, 68, 80),
                Day(3, 22, 64, 76),
                Day(5, 29, 52, 65),
                Day(7, 19, 47, 62),
                Day(9, 22, 50, 65),
                Day(11, 26, 63, 76)
            ),
            Place(
                "Antarctica",
                -77.846,
                166.676,
                446,
                Day(1, 5, 25, 33),
                Day(4, 18, -10, -1),
                Day(8, 11, -18, -9),
                Day(9, 28, -12, -1),
                Day(11, 22, 16, 24)
            )
        )

        for (place in places) {
            for (day in place.days) {
                var actual = subsystem.getTemperatureRange(
                    LocalDate.of(2022, day.month, day.day),
                    Coordinate(place.latitude, place.longitude),
                    Distance.feet(place.elevation.toFloat()),
                    false
                )
                actual = actual.copy(
                    start = actual.start.convertTo(TemperatureUnits.F),
                    end = actual.end.convertTo(TemperatureUnits.F)
                )
                val expected = Range(
                    Temperature(day.low.toFloat(), TemperatureUnits.F),
                    Temperature(day.high.toFloat(), TemperatureUnits.F)
                )

                errors.add(actual.start.temperature - expected.start.temperature)
                errors.add(actual.end.temperature - expected.end.temperature)

                assertEquals(
                    place.name,
                    expected.start.temperature,
                    actual.start.temperature,
                    maximumError
                )
                assertEquals(
                    place.name,
                    expected.end.temperature,
                    actual.end.temperature,
                    maximumError
                )
            }
        }


        // Check the average error and standard deviation
        val averageError = Statistics.mean(errors)
        val absAverageError = Statistics.mean(errors.map { abs(it) })
        val standardDeviation = Statistics.stdev(errors.map { abs(it) })

        assertTrue("Average Tends Colder", averageError <= 0)
        assertEquals("Average", 0f, absAverageError, maximumAverageError)
        assertEquals("Standard Deviation", 0f, standardDeviation, maximumStdDevError)
    }

    private class Place(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Int,
        vararg val days: Day
    )

    private class Day(
        val month: Int,
        val day: Int,
        val low: Int,
        val high: Int
    )

}