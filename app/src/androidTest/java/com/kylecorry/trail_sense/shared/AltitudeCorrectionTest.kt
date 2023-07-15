package com.kylecorry.trail_sense.shared

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Coordinate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class AltitudeCorrectionTest {

    @Test
    fun getGeoid() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val errors = mutableListOf<Float>()
        val maximumError = 6f
        val maximumAverageError = 1.5f
        val maximumStdDevError = 2f

        val places = listOf(
            Place("New York", 41.714, -74.006, -32.57f),
            Place("Orlando", 28.538, -81.379, -29.2f),
            Place("Los Angeles", 34.052, -118.244, -35.17f),
            Place("Quito", -0.230, -78.525, 26.89f),
            Place("London", 51.509, -0.126, 46.09f),
            Place("Anchorage", 61.218, -149.900, 8.05f),
            Place("Amesterdam", 52.374, 4.890, 43.19f),
            Place("Stockholm", 59.333, 18.065, 23.2141f),
            Place("Rio de Janeiro", -22.903, -43.208, -5.7334f),
            Place("Honolulu", 21.307, -157.858, 15.8073f),
            Place("Tokyo", 35.689, 139.692, 36.7074f),
            Place("Bangkok", 13.754, 100.501, -31.4845f),
            Place("Sydney", -33.868, 151.207, 22.3619f)
        )

        for (place in places) {
            val geoid = AltitudeCorrection.getGeoid(context, Coordinate(place.latitude, place.longitude))
            assertEquals(place.offset, geoid, maximumError)
            errors.add(geoid - place.offset)
        }

        // Check the average error and standard deviation
        val absAverageError = Statistics.mean(errors.map { abs(it) })
        val standardDeviation = Statistics.stdev(errors.map { abs(it) })

        assertEquals("Average", 0f, absAverageError, maximumAverageError)
        assertEquals("Standard Deviation", 0f, standardDeviation, maximumStdDevError)
    }

    private class Place(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val offset: Float
    )

}