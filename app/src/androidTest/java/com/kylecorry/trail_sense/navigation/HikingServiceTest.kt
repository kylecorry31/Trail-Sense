package com.kylecorry.trail_sense.tools.navigation

import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.absoluteValue

class HikingServiceTest {

    @Test
    fun getDifficulty() = runBlocking {
        val hikingService = HikingService()

        val paths = mapOf(
            // Short with a steep finish (USGS shows 12 m of climb in the last 50 m)
            "paths/durfee short.gpx" to HikingDifficulty.Moderate,
            // Medium length and flat
            "paths/sprague.gpx" to HikingDifficulty.Moderate,
            // Short and flat, the GPS elevations are noisy but the terrain is not (verified with USGS)
            "paths/durfee loop.gpx" to HikingDifficulty.Easy,
            // Long and flat
            "paths/pulaski.gpx" to HikingDifficulty.Hard,
            // Long and steep
            "paths/mount mansfield.gpx" to HikingDifficulty.Hard,
            // Medium length with over 750 ft of gain
            "paths/moosilauke.gpx" to HikingDifficulty.Hard
        )

        for ((key, value) in paths) {
            // Correct elevations to match how they would be in the app
            val points = hikingService.correctElevations(TestData.loadGpxPoints(key))
            val difficulty = hikingService.getHikingDifficulty(points)
            assertEquals(value, difficulty)
        }
    }

    @Test
    fun correctedElevationsMatchUsgs() {
        val hikingService = HikingService()
        val elevationErrors = mutableListOf<Float>()
        val gainLossErrors = mutableListOf<Float>()

        // The USGS paths are the recorded paths with the elevations replaced by the USGS 3DEP elevation (scripts/fill_usgs_elevations.py)
        for (name in USGS_PATHS) {
            val corrected = hikingService.correctElevations(TestData.loadGpxPoints("paths/$name.gpx"))
            val usgs = TestData.loadGpxPoints("paths/usgs/$name.gpx")

            elevationErrors.addAll(corrected.indices.map { corrected[it].elevation!! - usgs[it].elevation!! })

            val (loss, gain) = hikingService.getElevationLossGain(corrected)
            val (usgsLoss, usgsGain) = hikingService.getElevationLossGain(usgs)
            val gainError = gain.meters().value - usgsGain.meters().value
            val lossError = usgsLoss.meters().value - loss.meters().value
            val totalChange = usgsGain.meters().value - usgsLoss.meters().value
            gainLossErrors.add((gainError.absoluteValue + lossError.absoluteValue) / totalChange)
        }

        // Meters
        assertQuantile(elevationErrors, 3f, 0.5f, "Elevation")
        assertQuantile(elevationErrors, 16f, 0.9f, "Elevation")
        // Fraction of the USGS gain plus loss
        assertQuantile(gainLossErrors, 0.15f, 0.5f, "Gain/loss")
        assertQuantile(gainLossErrors, 0.25f, 0.9f, "Gain/loss")
    }

    private companion object {
        // Sprague and Pulaski were recorded with elevations that already match USGS, so any error there is from the smoothing.
        // Some of the others have an elevation that drifts by tens of meters, which smoothing can't fix.
        val USGS_PATHS = listOf(
            "durfee loop",
            "durfee short",
            "sprague",
            "pulaski",
            "mount mansfield",
            "moosilauke",
            "tuckermans ravine",
            "dianas bath",
            "mt chocorua",
            "moat mountain",
            "cathedral ledge",
            "presidential range"
        )
    }

}
