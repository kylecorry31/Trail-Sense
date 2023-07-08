package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.time.Time.utc
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.domain.selection.NearestTideSelectionStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

internal class NearestTideSelectionStrategyTest {

    @Test
    fun getTideMultiple() = runBlocking {
        val strategy = NearestTideSelectionStrategy { Coordinate.zero }
        val tides = listOf(
            table(1, 100, Coordinate(1.0, 1.0)),
            table(2, 100, Coordinate(0.1, 1.0)),
            table(3, 100, Coordinate(2.0, 1.0)),
            table(4, 100, null),
        )

        Assertions.assertEquals(tides[1], strategy.getTide(tides))
    }

    @Test
    fun getTideNoCoords() = runBlocking {
        val strategy = NearestTideSelectionStrategy { Coordinate.zero }
        val tides = listOf(
            table(4, 10, null)
        )

        Assertions.assertNull(strategy.getTide(tides))
    }

    @Test
    fun getTideOneLocation() = runBlocking {
        val strategy = NearestTideSelectionStrategy { Coordinate.zero }
        val tides = listOf(
            table(1, 100, Coordinate(1.0, 1.0))
        )

        Assertions.assertEquals(tides[0], strategy.getTide(tides))
    }

    private fun table(id: Long, millis: Long, location: Coordinate?): TideTable {
        return TideTable(id, listOf(Tide.high(Instant.ofEpochMilli(millis).utc())), null, location)
    }
}