package com.kylecorry.trail_sense.tools.signal_finder

import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CellTowerModelTest {

    @Test
    fun getTowers() = runBlocking {
        val tests = listOf(
            Coordinate(42.03, -71.97) to listOf(CellNetwork.Lte),
            Coordinate(42.0, -72.0) to listOf(),
            Coordinate(45.09, -61.8) to listOf(CellNetwork.Lte),
            Coordinate(27.03, 14.43) to listOf(CellNetwork.Wcdma, CellNetwork.Lte),
            Coordinate(-22.53, -55.74) to listOf(CellNetwork.Lte),
        )

        for (test in tests) {
            val (coordinate, networks) = test
            val towers = CellTowerModel.getTowers(context, coordinate)
            assertEquals(networks, towers.second)
        }
    }
}