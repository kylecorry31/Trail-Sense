package com.kylecorry.trail_sense.tools.signal_finder

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CellTowerModelTest {

    @Test
    fun getTowers() = runBlocking {
        // TODO: Re-add tests
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
//        AppServiceRegistry.register(FileSubsystem.getInstance(context))
//        val tests = listOf(
//            Coordinate(42.03, -71.97) to true,
//            Coordinate(42.0, -72.0) to false,
//            Coordinate(45.09, -61.8) to true,
//            Coordinate(27.03, 14.43) to true,
//            Coordinate(-22.53, -55.74) to true,
//        )
//
//        for (test in tests) {
//            val (coordinate, expected) = test
//            val towers = CellTowerModel.getTowers(Geofence(coordinate, Distance.miles(1f)))
//            assertEquals(expected, towers.contains(coordinate))
//        }
    }
}