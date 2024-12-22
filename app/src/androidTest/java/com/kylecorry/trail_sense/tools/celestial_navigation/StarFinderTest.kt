package com.kylecorry.trail_sense.tools.celestial_navigation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.AltitudeAzimuth
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.StarFinderFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class StarFinderTest {

    @Test
    fun findStars() = runBlocking {

        val expectedStarsMap = mapOf(
            "stars/20241215_020631.jpg" to listOf(
                Star.Alnilam,
                Star.Mintaka,
                Star.Alnitak,
                Star.Betelgeuse,
                Star.Rigel,
                Star.Bellatrix,
//                Star.Saiph
            ),
            "stars/stars-1734823961245-1.1529049-73.340225-28.897032-36.548954.webp" to listOf(
                Star.Schedar,
                Star.Ruchbah
            ),
            "stars/stars-1734824061799-7.066048-73.54161-20.092363-25.412811.webp" to listOf(
                Star.Schedar
            )
        )

        val assets = AssetFileSystem(InstrumentationRegistry.getInstrumentation().context)
        val files = assets.list("stars").sorted().map { "stars/$it" }

        for (file in files) {
            val image = assets.stream(file).use {
                BitmapFactory.decodeStream(it)
            }

            var azimuth = 0f
            var altitude = 0f
            var time = ZonedDateTime.of(2024, 12, 15, 2, 0, 0, 0, ZoneId.of("America/New_York"))
            var fovWidth = 8.566184f
            var fovHeight = 6.77276f

            if (file.contains("-")) {
                val parts = file.replace(".webp", "").split("-")
                time = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(parts[1].toLong()),
                    ZoneId.of("America/New_York")
                )
                azimuth = parts[2].toFloat()
                altitude = parts[3].toFloat()
                // Images are in portrait, so flip the FOV
                fovWidth = parts[5].toFloat()
                fovHeight = parts[4].toFloat()
            }

            val stars = StarFinderFactory().getStarFinder().findStars(image)

            var debugImage = image.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(debugImage)
            val drawer = CanvasDrawer(context, canvas)
            for (star in stars) {
                drawer.stroke(Color.RED)
                drawer.strokeWeight(4f)
                drawer.noFill()
                drawer.circle(star.x, star.y, 40f)
            }
            debugImage = debugImage.resizeToFit(1000, 1000)

            assert(stars.isNotEmpty())
            val starReadings = stars.map {
                val xAngle = ((it.x - image.width / 2) / image.width.toFloat()) * fovWidth + azimuth
                val yAngle =
                    -((it.y - image.height / 2) / image.height.toFloat()) * fovHeight + altitude
                AltitudeAzimuth(yAngle, xAngle)
            }

            val plate =
                Astronomy.plateSolve(
                    starReadings,
                    time,
                    tolerance = 0.1f
                )

            val expected = expectedStarsMap[file]
            if (expected?.isNotEmpty() == true) {
                assertEquals(file, expected.size, plate.size)
                assertTrue(file, plate.map { it.star }.containsAll(expected))
            }
            println(stars)
            debugImage.recycle()
        }
    }

}