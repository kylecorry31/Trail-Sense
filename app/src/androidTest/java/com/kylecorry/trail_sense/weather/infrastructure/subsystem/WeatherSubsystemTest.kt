package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.Month

internal class WeatherSubsystemTest {

    @Test
    fun temperature() = runBlocking {
        // Source of truth: https://weatherspark.com/
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        val maximumError = 10.5f
        val maxQuantile50Error = 2f
        val maxQuantile90Error = 6.5f

        val errors = mutableListOf<Float>()

        val places = listOf(
            TemperaturePlace(
                "New York",
                41.714,
                -74.006,
                30,
                TemperatureDay(1, 29, 28, 39),
                TemperatureDay(3, 12, 34, 48),
                TemperatureDay(6, 2, 61, 76),
                TemperatureDay(7, 21, 71, 85),
                TemperatureDay(9, 16, 63, 76),
                TemperatureDay(12, 3, 37, 48)
            ),
            TemperaturePlace(
                "Orlando",
                28.538,
                -81.379,
                98,
                TemperatureDay(1, 15, 52, 70),
                TemperatureDay(2, 25, 55, 74),
                TemperatureDay(5, 15, 69, 87),
                TemperatureDay(7, 22, 76, 91),
                TemperatureDay(9, 28, 73, 87),
                TemperatureDay(12, 4, 58, 74)
            ),
            TemperaturePlace(
                "Los Angeles",
                34.052,
                -118.244,
                289,
                TemperatureDay(3, 23, 53, 70),
                TemperatureDay(7, 1, 64, 81),
                TemperatureDay(8, 25, 66, 85),
                TemperatureDay(9, 30, 63, 81),
                TemperatureDay(12, 25, 48, 67)
            ),
//            Place(
//                "Quito",
//                -0.230,
//                -78.525,
//                9364,
//                Day(2, 2, 49, 64),
//                Day(4, 25, 49, 64),
//                Day(7, 16, 48, 65),
//                Day(9, 20, 48, 66)
//            ),
            TemperaturePlace(
                "London",
                51.509,
                -0.126,
                82,
                TemperatureDay(2, 7, 39, 47),
                TemperatureDay(3, 18, 42, 53),
                TemperatureDay(6, 15, 55, 69),
                TemperatureDay(9, 1, 60, 74),
                TemperatureDay(9, 7, 56, 69),
                TemperatureDay(11, 16, 44, 53)
            ),
            TemperaturePlace(
                "Anchorage",
                61.218,
                -149.900,
                98,
                TemperatureDay(1, 17, 13, 23),
                TemperatureDay(3, 5, 17, 32),
                TemperatureDay(7, 20, 55, 68),
                TemperatureDay(9, 11, 46, 59),
                TemperatureDay(11, 6, 22, 32)
            ),
            TemperaturePlace(
                "Amesterdam",
                52.374,
                4.890,
                43,
                TemperatureDay(2, 8, 34, 42),
                TemperatureDay(3, 13, 37, 48),
                TemperatureDay(6, 11, 51, 65),
                TemperatureDay(8, 5, 57, 71),
                TemperatureDay(9, 13, 52, 65),
                TemperatureDay(11, 20, 39, 48)
            ),
            TemperaturePlace(
                "Stockholm",
                59.333,
                18.065,
                95,
                TemperatureDay(2, 8, 22, 32),
                TemperatureDay(3, 19, 27, 40),
                TemperatureDay(6, 4, 47, 64),
                TemperatureDay(7, 22, 56, 72),
                TemperatureDay(9, 4, 49, 64),
                TemperatureDay(11, 19, 32, 40)
            ),
            TemperaturePlace(
                "Rio de Janeiro",
                -22.903,
                -43.208,
                16,
                TemperatureDay(2, 12, 76, 88),
                TemperatureDay(5, 19, 69, 79),
                TemperatureDay(7, 22, 65, 77),
                TemperatureDay(10, 10, 69, 79),
                TemperatureDay(12, 25, 75, 86)
            ),
            TemperaturePlace(
                "Honolulu",
                21.307,
                -157.858,
                59,
                TemperatureDay(1, 28, 68, 79),
                TemperatureDay(4, 1, 70, 81),
                TemperatureDay(6, 27, 75, 85),
                TemperatureDay(9, 23, 76, 87),
                TemperatureDay(10, 14, 75, 85),
                TemperatureDay(12, 13, 71, 81)
            ),
            TemperaturePlace(
                "Tokyo",
                35.689,
                139.692,
                141,
                TemperatureDay(1, 26, 36, 48),
                TemperatureDay(3, 20, 43, 56),
                TemperatureDay(6, 27, 70, 79),
                TemperatureDay(9, 9, 77, 87),
                TemperatureDay(9, 18, 70, 79),
                TemperatureDay(12, 4, 44, 56)
            ),
            TemperaturePlace(
                "Bangkok",
                13.754,
                100.501,
                39,
                TemperatureDay(1, 1, 71, 88),
                TemperatureDay(4, 15, 80, 95),
                TemperatureDay(10, 12, 78, 89)
            ),
            TemperaturePlace(
                "Sydney",
                -33.868,
                151.207,
                190,
                TemperatureDay(1, 25, 68, 80),
                TemperatureDay(3, 22, 64, 76),
                TemperatureDay(5, 29, 52, 65),
                TemperatureDay(7, 19, 47, 62),
                TemperatureDay(9, 22, 50, 65),
                TemperatureDay(11, 26, 63, 76)
            ),
//            Place(
//                "Antarctica",
//                -77.846,
//                166.676,
//                446,
//                Day(1, 5, 25, 33),
//                Day(4, 18, -10, -1),
//                Day(8, 11, -18, -9),
//                Day(9, 28, -12, -1),
//                Day(11, 22, 16, 24)
//            )
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

        assertQuantile(errors, maxQuantile50Error, 0.5f, "Temperature")
        assertQuantile(errors, maxQuantile90Error, 0.9f, "Temperature")
    }

    @Test
    fun precipitation() = runBlocking {
        // Source of truth: https://weatherspark.com/
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val subsystem = WeatherSubsystem.getInstance(context)

        val maximumError = 2f
        val maxQuantile50Error = 0.5f
        val maxQuantile90Error = 1f

        val errors = mutableListOf<Float>()

        val places = listOf(
            PrecipitationPlace(
                "New York",
                41.714,
                -74.006,
                mapOf(
                    Month.JANUARY to 2.3f + 0.61f,
                    Month.FEBRUARY to 2.2f + 0.61f,
                    Month.MARCH to 3.2f + 0.3f,
                    Month.APRIL to 3.7f,
                    Month.MAY to 3.6f,
                    Month.JUNE to 3.6f,
                    Month.JULY to 3.5f,
                    Month.AUGUST to 3.6f,
                    Month.SEPTEMBER to 3.5f,
                    Month.OCTOBER to 3.5f,
                    Month.NOVEMBER to 3.4f + 0.04f,
                    Month.DECEMBER to 3.3f + 0.3f
                )
            ),
            PrecipitationPlace(
                "Orlando",
                28.538,
                -81.379,
                mapOf(
                    Month.JANUARY to 2.3f,
                    Month.FEBRUARY to 2.4f,
                    Month.MARCH to 2.9f,
                    Month.APRIL to 2.4f,
                    Month.MAY to 2.9f,
                    Month.JUNE to 5.7f,
                    Month.JULY to 5.6f,
                    Month.AUGUST to 5.6f,
                    Month.SEPTEMBER to 5.1f,
                    Month.OCTOBER to 2.8f,
                    Month.NOVEMBER to 1.8f,
                    Month.DECEMBER to 1.9f
                )
            ),
            PrecipitationPlace(
                "Los Angeles",
                34.052,
                -118.244,
                mapOf(
                    Month.JANUARY to 2.6f,
                    Month.FEBRUARY to 3.3f,
                    Month.MARCH to 2.1f,
                    Month.APRIL to 0.8f,
                    Month.MAY to 0.2f,
                    Month.JUNE to 0.1f,
                    Month.JULY to 0.0f,
                    Month.AUGUST to 0.0f,
                    Month.SEPTEMBER to 0.2f,
                    Month.OCTOBER to 0.5f,
                    Month.NOVEMBER to 1.0f,
                    Month.DECEMBER to 2.1f
                )
            ),
            PrecipitationPlace(
                "London",
                51.509,
                -0.126,
                mapOf(
                    Month.JANUARY to 1.8f,
                    Month.FEBRUARY to 1.4f,
                    Month.MARCH to 1.2f,
                    Month.APRIL to 1.2f,
                    Month.MAY to 1.5f,
                    Month.JUNE to 1.6f,
                    Month.JULY to 1.4f,
                    Month.AUGUST to 1.5f,
                    Month.SEPTEMBER to 1.6f,
                    Month.OCTOBER to 2.2f,
                    Month.NOVEMBER to 2.1f,
                    Month.DECEMBER to 2.0f
                )
            ),
            PrecipitationPlace(
                "Bangkok",
                13.754,
                100.501,
                mapOf(
                    Month.JANUARY to 0.2f,
                    Month.FEBRUARY to 0.4f,
                    Month.MARCH to 1.0f,
                    Month.APRIL to 2.2f,
                    Month.MAY to 4.5f,
                    Month.JUNE to 4.4f,
                    Month.JULY to 4.1f,
                    Month.AUGUST to 5.0f,
                    Month.SEPTEMBER to 8.6f,
                    Month.OCTOBER to 6.9f,
                    Month.NOVEMBER to 1.8f,
                    Month.DECEMBER to 0.2f
                )
            )
        )

        for (place in places) {
            val all = subsystem.getMonthlyPrecipitation(Coordinate(place.latitude, place.longitude))
            for (month in place.months) {
                val actual = all[month.key]!!.convertTo(DistanceUnits.Inches).distance
                val expected = month.value

                errors.add(actual - expected)

                assertEquals(
                    place.name + " - " + month.key,
                    expected,
                    actual,
                    maximumError
                )
            }
        }

        assertQuantile(errors, maxQuantile50Error, 0.5f, "Precipitation")
        assertQuantile(errors, maxQuantile90Error, 0.9f, "Precipitation")
    }

    private class TemperaturePlace(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Int,
        vararg val days: TemperatureDay
    )

    private class TemperatureDay(
        val month: Int,
        val day: Int,
        val low: Int,
        val high: Int
    )

    private class PrecipitationPlace(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val months: Map<Month, Float>
    )


}