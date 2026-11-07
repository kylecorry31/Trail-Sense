package com.kylecorry.trail_sense.tools.signal_finder

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertWithin
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class CellTowerModelTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(context))
    }

    @Test
    fun getTowers() = runBlocking {
        // Reference: towers from the source data (FCC, OpenCelliD, and MLS) from generate-cell-tower-test-data.py in
        // the Earth Model project, so this shows the error added by merging the towers into the model. The error is the
        // distance to the closest tower in the model.
        val errors = loadPoints().map { tower ->
            val closest = CellTowerModel.getTowers(
                CoordinateBounds.from(Geofence(tower, Distance.kilometers(5f))),
                1
            ).firstOrNull()
            closest?.coordinate?.distanceTo(tower) ?: Float.MAX_VALUE
        }

        assertQuantile(errors, 500f, 0.5f, "Cell towers")
        assertQuantile(errors, 650f, 0.9f, "Cell towers")
        assertWithin(errors, 1000f, 0.99f, "Cell towers")
    }

    private fun loadPoints(): List<Coordinate> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return assets.open("cell_towers_tests/points.csv").bufferedReader().readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val row = line.split(",")
                Coordinate(row[0].toDouble(), row[1].toDouble())
            }
    }
}
