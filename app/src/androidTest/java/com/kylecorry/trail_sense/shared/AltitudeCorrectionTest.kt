package com.kylecorry.trail_sense.shared

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertBias
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertWithin
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class AltitudeCorrectionTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(context))
    }

    @Test
    fun geoidModel() = runBlocking {
        // Reference: the full resolution ETOPO 2022 geoid (m) at points in hiking areas, across the world, on a grid,
        // and at the extremes (the poles, the antimeridian, and where the geoid is highest, lowest, and steepest),
        // from generate-geoid-test-data.py in the Earth Model project, so this shows the error added by shrinking
        // the model.
        val errors = loadPoints().map { AltitudeCorrection.getGeoid(it.location) - it.geoid }

        assertQuantile(errors, 0.6f, 0.5f, "Geoid")
        assertQuantile(errors, 1.8f, 0.9f, "Geoid")
        assertQuantile(errors, 3.5f, 0.99f, "Geoid")
        assertWithin(errors, 3f, 0.9f, "Geoid")
        assertBias(errors, 0.2f, "Geoid")
    }

    private fun loadPoints(): List<Point> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return assets.open("geoid_tests/points.csv").bufferedReader().readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val row = line.split(",")
                Point(Coordinate(row[0].toDouble(), row[1].toDouble()), row[2].toFloat())
            }
    }

    private class Point(val location: Coordinate, val geoid: Float)

}
