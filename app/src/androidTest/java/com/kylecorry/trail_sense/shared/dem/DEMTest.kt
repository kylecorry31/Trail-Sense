package com.kylecorry.trail_sense.shared.dem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class DEMTest {

    private data class Model(
        val path: String,
        val maxQuantile50Error: Float,
        val maxQuantile90Error: Float
    )

    private val models = listOf(
        Model("dem/dem-0.2.0-mini.zip", 20f, 175f),
        Model("dem/dem-0.2.0-low.zip", 20f, 60f),
        Model("dem/dem-0.2.0-medium.zip", 15f, 45f),
        Model("dem/dem-0.2.0-high.zip", 5f, 45f),
    )

    @Test
    fun getElevation() {
        for (model in models) {
            println("Testing ${model.path}")
            verify(model)
        }
    }

    private fun verify(model: Model) {
        val assets = AssetFileSystem(InstrumentationRegistry.getInstrumentation().context)
        val cache = CacheFileSystem(context)
        if (!assets.list("dem").contains(model.path.replace("dem/", ""))) {
            println("Skipping ${model.path} because it does not exist in the assets")
            return
        }
        runBlocking {
            cache.outputStream("dem.zip").use { output ->
                assets.stream(model.path).use { input ->
                    input.copyTo(output)
                }
            }

            DigitalElevationModelLoader().load(cache.getUri("dem.zip"))
        }

        // https://elevation.maplogs.com/
        val tests = listOf(
            Coordinate(41.988, -71.733) to 197f,
            Coordinate(32.146, -81.332) to 12f,
            Coordinate(45.425, 9.611) to 90f,
            Coordinate(33.599, -111.768) to 606f,
            Coordinate(47.339, 8.529) to 456f,
            Coordinate(5.314, 6.878) to 47f,
            Coordinate(42.079, -80.06) to 308f,
            Coordinate(35.168, -83.278) to 985f,
            Coordinate(-16.513, -68.129) to 3594f,
            Coordinate(45.339, 22.894) to 1615f,
            Coordinate(48.672, -113.804) to 1098f,
            Coordinate(41.809, -72.467) to 161f,
            Coordinate(30.926, 78.233) to 2472f,
            Coordinate(43.57, -96.503) to 410f,
            Coordinate(51.519, 0.040) to 1f,
            Coordinate(0.0, -121.0) to 0f,
            Coordinate(37.541, -122.506) to 51f,
            Coordinate(27.185, 56.283) to 6f,
            Coordinate(-29.913, 151.111) to 777f,
            Coordinate(54.676, 32.137) to 212f,
            Coordinate(-4.036, -61.857) to 28f,
            Coordinate(-42.008, -69.025) to 1284f,
            Coordinate(19.563, -155.721) to 2031f,
            Coordinate(65.172, -157.565) to 47f,
            Coordinate(37.641, 81.807) to 1394f,
            Coordinate(-0.074, 115.814) to 45f
        )

        val errors = tests.map { test ->
            val actual = runBlocking { DEM.getElevation(test.first) }
            assertNotNull(actual)
            actual!!.meters().distance - test.second
        }

        assertQuantile(errors, model.maxQuantile50Error, 0.5f, model.path)
        assertQuantile(errors, model.maxQuantile90Error, 0.9f, model.path)
    }

}