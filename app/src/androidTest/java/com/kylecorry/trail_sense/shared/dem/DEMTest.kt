package com.kylecorry.trail_sense.shared.dem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertBias
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertWithin
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class DEMTest {

    private data class Model(
        val path: String?,
        // Land points in hiking areas
        val maxQuantile50Error: Float,
        val maxQuantile90Error: Float,
        // Minimum fraction of land points within WITHIN_NEAR_METERS and WITHIN_FAR_METERS
        val minWithinNear: Float,
        val minWithinFar: Float,
        // Maximum median signed error, which shows a systematic high or low
        val maxBias: Float,
        // Maximum 90th percentile error by terrain
        val maxMountainsQuantile90Error: Float,
        val maxHillsQuantile90Error: Float,
        val maxFlatQuantile90Error: Float,
        // Maximum median error at well known peaks
        val maxPeakQuantile50Error: Float,
        // Minimum DEM elevation gain and loss along a route, as a fraction of the reference (median and lower quartile)
        val minMedianGainRatio: Float,
        val minLowerQuartileGainRatio: Float,
        // Maximum amount the 90th percentile step across a tile boundary can exceed the same away from a boundary
        val maxSeamStepExcess: Float
    )

    private val models = listOf(
        Model(
            path = null,
            maxQuantile50Error = 35f,
            maxQuantile90Error = 181f,
            minWithinNear = 0.4f,
            minWithinFar = 0.78f,
            maxBias = 1.3f,
            maxMountainsQuantile90Error = 200f,
            maxHillsQuantile90Error = 148f,
            maxFlatQuantile90Error = 26f,
            maxPeakQuantile50Error = 121f,
            minMedianGainRatio = 0.51f,
            minLowerQuartileGainRatio = 0.33f,
            maxSeamStepExcess = 59f
        ),
        Model(
            path = "dem/dem-0.6.0-low.zip",
            maxQuantile50Error = 21f,
            maxQuantile90Error = 92f,
            minWithinNear = 0.54f,
            minWithinFar = 0.92f,
            maxBias = 1.4f,
            maxMountainsQuantile90Error = 101f,
            maxHillsQuantile90Error = 76f,
            maxFlatQuantile90Error = 20f,
            maxPeakQuantile50Error = 175f,
            minMedianGainRatio = 0.77f,
            minLowerQuartileGainRatio = 0.58f,
            maxSeamStepExcess = 13f
        ),
        Model(
            path = "dem/dem-0.6.0-medium.zip",
            maxQuantile50Error = 13f,
            maxQuantile90Error = 46f,
            minWithinNear = 0.75f,
            minWithinFar = 0.98f,
            maxBias = 2.7f,
            maxMountainsQuantile90Error = 50f,
            maxHillsQuantile90Error = 41f,
            maxFlatQuantile90Error = 17f,
            maxPeakQuantile50Error = 139f,
            minMedianGainRatio = 0.88f,
            minLowerQuartileGainRatio = 0.78f,
            maxSeamStepExcess = 10f
        ),
        Model(
            path = "dem/dem-0.6.0-high.zip",
            maxQuantile50Error = 8f,
            maxQuantile90Error = 41f,
            minWithinNear = 0.79f,
            minWithinFar = 0.99f,
            maxBias = 1.6f,
            maxMountainsQuantile90Error = 48f,
            maxHillsQuantile90Error = 34f,
            maxFlatQuantile90Error = 14f,
            maxPeakQuantile50Error = 137f,
            minMedianGainRatio = 0.88f,
            minLowerQuartileGainRatio = 0.78f,
            maxSeamStepExcess = 9f
        ),
    )

    @Test
    fun getElevation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(context))
        val data = runBlocking { DemTestData.load() }
        for (model in models) {
            println("Testing ${model.path}")
            if (load(model)) {
                verify(model, data)
            }
        }
    }

    private fun load(model: Model): Boolean {
        val assets = AssetFileSystem(InstrumentationRegistry.getInstrumentation().context)
        val cache = CacheFileSystem(context)
        if (model.path != null && !assets.list("dem").contains(model.path.replace("dem/", ""))) {
            println("Skipping ${model.path} because it does not exist in the assets")
            return false
        }
        runBlocking {
            if (model.path != null) {
                cache.outputStream("dem.zip").use { output ->
                    assets.stream(model.path).use { input ->
                        input.copyTo(output)
                    }
                }

                DigitalElevationModelLoader().load(cache.getUri("dem.zip")).collect()
            } else {
                DigitalElevationModelLoader().clear()
            }
        }
        return true
    }

    private fun verify(model: Model, data: DemTestData) {
        val name = model.path ?: "Built-in"
        verifyPoints(model, data, name)
        verifyPeaks(model, data, name)
        verifyRoutes(model, data, name)
        verifySeams(model, data, name)
    }

    private fun verifyPoints(model: Model, data: DemTestData, name: String) {
        val terrains = listOf("mountains", "hills", "flat")
        val errors = terrains.associateWith { getErrors(data.points[it] ?: emptyList()) }
        val land = errors.values.flatten()

        assertQuantile(land, model.maxQuantile50Error, 0.5f, name)
        assertQuantile(land, model.maxQuantile90Error, 0.9f, name)
        assertWithin(land, WITHIN_NEAR_METERS, model.minWithinNear, name)
        assertWithin(land, WITHIN_FAR_METERS, model.minWithinFar, name)
        assertBias(land, model.maxBias, name)
        assertQuantile(errors.getValue("mountains"), model.maxMountainsQuantile90Error, 0.9f, "$name mountains")
        assertQuantile(errors.getValue("hills"), model.maxHillsQuantile90Error, 0.9f, "$name hills")
        assertQuantile(errors.getValue("flat"), model.maxFlatQuantile90Error, 0.9f, "$name flat")

        val ocean = getErrors(data.points["ocean"] ?: emptyList())
        assertQuantile(ocean, MAX_OCEAN_ERROR_METERS, 1f, "$name ocean")
    }

    private fun verifyPeaks(model: Model, data: DemTestData, name: String) {
        // Official summit elevations, which show how much of a summit the model keeps
        val peaks = getErrors(data.points["peaks"] ?: emptyList())
        assertQuantile(peaks, model.maxPeakQuantile50Error, 0.5f, "$name peaks")
    }

    private fun verifyRoutes(model: Model, data: DemTestData, name: String) {
        val ratios = data.routes.flatMap { (route, points) ->
            val expected = gainAndLoss(points.map { it.second })
            val actual = gainAndLoss(points.map { getElevation(it.first) })
            println("$name $route: gain ${actual.first} / ${expected.first}, loss ${actual.second} / ${expected.second}")
            listOfNotNull(
                (actual.first / expected.first).takeIf { expected.first >= MIN_REFERENCE_CHANGE_METERS },
                (actual.second / expected.second).takeIf { expected.second >= MIN_REFERENCE_CHANGE_METERS }
            )
        }.sorted()
        val median = Statistics.quantile(ratios, 0.5f)
        val lowerQuartile = Statistics.quantile(ratios, 0.25f)
        println("$name route gain and loss ratio: median $median, lower quartile $lowerQuartile, highest ${ratios.last()}")
        assertTrue("$name route median ratio $median", median >= model.minMedianGainRatio)
        assertTrue("$name route lower quartile ratio $lowerQuartile", lowerQuartile >= model.minLowerQuartileGainRatio)
        assertTrue("$name route highest ratio ${ratios.last()}", ratios.last() <= MAX_GAIN_RATIO)
    }

    private fun verifySeams(model: Model, data: DemTestData, name: String) {
        // How much further apart the DEM puts two nearby points than they really are
        fun steps(kind: String): List<Float> {
            return (data.seams[kind] ?: emptyList()).map { (a, b) ->
                val expected = abs(a.second - b.second)
                val actual = abs(getElevation(a.first) - getElevation(b.first))
                max(actual - expected, 0f)
            }
        }

        val seam = Statistics.quantile(steps("seam"), 0.9f)
        val control = Statistics.quantile(steps("control"), 0.9f)
        println("$name seam step 90th percentile: $seam, away from a boundary: $control")
        assertTrue("$name seam step $seam vs $control", seam - control <= model.maxSeamStepExcess)
    }

    private fun getElevation(location: Coordinate): Float {
        val elevation = runBlocking { DEM.getElevation(location) }
        assertNotNull(elevation)
        return elevation.elevation
    }

    private fun getErrors(points: List<Pair<Coordinate, Float>>): List<Float> {
        return points.map { getElevation(it.first) - it.second }
    }

    // Changes smaller than the threshold are ignored, as they are mostly noise
    private fun gainAndLoss(elevations: List<Float>): Pair<Float, Float> {
        var reference = elevations.first()
        var gain = 0f
        var loss = 0f
        for (elevation in elevations.drop(1)) {
            if (elevation - reference >= GAIN_THRESHOLD_METERS) {
                gain += elevation - reference
                reference = elevation
            } else if (reference - elevation >= GAIN_THRESHOLD_METERS) {
                loss += reference - elevation
                reference = elevation
            }
        }
        return gain to loss
    }

    companion object {
        private const val WITHIN_NEAR_METERS = 25f
        private const val WITHIN_FAR_METERS = 100f
        private const val MAX_OCEAN_ERROR_METERS = 6f
        private const val MAX_GAIN_RATIO = 1.5f
        private const val GAIN_THRESHOLD_METERS = 10f

        // Gain or loss below this on a route is too small to compare as a ratio
        private const val MIN_REFERENCE_CHANGE_METERS = 1000f
    }

}
