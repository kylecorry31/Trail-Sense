package com.kylecorry.trail_sense.tools.signal_finder

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
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

        AppServiceRegistry.register(FileSubsystem.getInstance(TestUtils.context))

        val errors = knownCellTowers.map { tower ->
            val towers =
                CellTowerModel.getTowers(CoordinateBounds.from(Geofence(tower, Distance.miles(5f))))
            val closest = towers.minBy { it.coordinate.distanceTo(tower) }
            closest.coordinate.distanceTo(tower)
        }

        assertQuantile(errors, 650f, 0.5f)
        assertQuantile(errors, 850f, 0.9f)
    }
}