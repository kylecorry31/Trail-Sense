package com.kylecorry.trail_sense.tools.navigation

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.gpx.GPXSerializer
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PathDifficultyTest {

    @Test
    fun getDifficulty() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val hikingService = HikingService()
        val assetFileSystem = AssetFileSystem(ctx)
        val serializer = GPXSerializer("Trail Sense")

        val paths = mapOf(
            // Short and flat
            "paths/durfee short.gpx" to HikingDifficulty.Easy,
            // Medium length and flat
            "paths/sprague.gpx" to HikingDifficulty.Moderate,
            // Short with a steep section
            "paths/durfee loop.gpx" to HikingDifficulty.Moderate,
            // Long and flat
            "paths/pulaski.gpx" to HikingDifficulty.Hard,
            // Long and steep
            "paths/mount mansfield.gpx" to HikingDifficulty.Hard
        )

        for ((key, value) in paths) {
            val gpx = assetFileSystem.stream(key).use {
                serializer.deserialize(it)
            }
            var points = gpx.tracks[0].segments[0].points.mapIndexed { index, point ->
                PathPoint(
                    index.toLong(),
                    0,
                    point.coordinate,
                    point.elevation,
                    point.time
                )
            }.sortedBy { it.id }
            // Correct elevations to match how they would be in the app
            points = hikingService.correctElevations(points)
            val difficulty = hikingService.getHikingDifficulty(points)
            assertEquals(value, difficulty)
        }
    }

}