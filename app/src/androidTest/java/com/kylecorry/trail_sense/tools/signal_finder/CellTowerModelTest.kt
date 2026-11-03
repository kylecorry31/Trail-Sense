package com.kylecorry.trail_sense.tools.signal_finder

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CellTowerModelTest {

    @Test
    fun getTowers() = runBlocking {
        val knownCellTowers = listOf(
            // RI
            Coordinate(41.887778, -71.755889),
            Coordinate(41.915583, -71.686472),
            Coordinate(41.966611, -71.755417),
            Coordinate(41.974194, -71.779944),
            // TODO: Add tests for other places
        )

        DependencyRegistry.addSingleton(FileSubsystem.getInstance(TestUtils.context))

        val errors = knownCellTowers.map { tower ->
            val towers =
                CellTowerModel.getTowers(CoordinateBounds.from(Geofence(tower, Distance.miles(5f))))
            val closest = towers.minBy { it.coordinate.distanceTo(tower) }
            closest.coordinate.distanceTo(tower)
        }

        assertQuantile(errors, 650f, 0.5f)
        assertQuantile(errors, 850f, 0.9f)
    }

    @Test
    fun getTowersNearRandomTowers() = runBlocking {
        // Reference: the exact location of 100 random towers from the model's sources, from
        // generate-cell-tower-test-data.py in the Earth Model project, so this shows the error added by storing
        // the towers in cells.
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(TestUtils.context))

        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val errors = assets.open("cell_tower_tests/points.csv").bufferedReader().readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val row = line.split(",")
                val tower = Coordinate(row[0].toDouble(), row[1].toDouble())
                val towers =
                    CellTowerModel.getTowers(CoordinateBounds.from(Geofence(tower, Distance.kilometers(5f))))
                towers.minOf { it.coordinate.distanceTo(tower) }
            }

        assertQuantile(errors, 250f, 0.5f, "Cell tower")
        assertQuantile(errors, 350f, 0.9f, "Cell tower")
        assertQuantile(errors, 420f, 0.99f, "Cell tower")
    }
}
