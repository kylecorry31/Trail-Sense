package com.kylecorry.trail_sense.tools.celestial_navigation

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.AltitudeAzimuth
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.DifferenceOfGaussiansStarFinder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StarFinderTest {

    @Test
    fun findStars() = runBlocking {
        val images = listOf(
            "stars/20241215_020532.jpg",
            "stars/20241215_020544.jpg",
            "stars/20241215_020631.jpg",
        )

        val expectedStars = listOf(
            emptyList(),
            emptyList(),
            listOf(
                Star.Alnilam,
                Star.Mintaka,
                Star.Alnitak,
                Star.Betelgeuse,
                Star.Rigel,
                Star.Bellatrix,
                Star.Saiph
            )
        )

        for (file in images) {
            val assets = AssetFileSystem(InstrumentationRegistry.getInstrumentation().context)
            val image = assets.stream(file).use {
                BitmapFactory.decodeStream(it)
            }

//            val stars = PercentOfMaxStarFinder(0.8f).findStars(image)
//            val stars = StandardDeviationStarFinder(5f).findStars(image)
            val stars = DifferenceOfGaussiansStarFinder(0.3f).findStars(image)
            assert(stars.isNotEmpty())
            val starReadings = stars.map {
                val xAngle = ((it.x - image.width / 2) / image.width.toFloat()) * 8.566184f
                val yAngle = -((it.y - image.height / 2) / image.height.toFloat()) * 6.77276f
                AltitudeAzimuth(yAngle, xAngle)
            }

            val plate =
                Astronomy.plateSolve(
                    starReadings,
                    ZonedDateTime.of(2024, 12, 15, 2, 0, 0, 0, ZoneId.of("America/New_York")),
                    tolerance = 0.1f
                )

            val expected = expectedStars[images.indexOf(file)]
            if (expected.isNotEmpty()) {
                assertEquals(expected.size, plate.size)
                assertTrue(plate.map { it.second }.containsAll(expected))
            }
        }
    }

}