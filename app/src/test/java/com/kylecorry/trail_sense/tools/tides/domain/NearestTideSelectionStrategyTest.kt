package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.time.Time.utc
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.domain.selection.NearestTideSelectionStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

internal class NearestTideSelectionStrategyTest {

    @Test
    fun getTideReadGPS() = runBlocking {
        val gps = mock<IGPS>()
        gps.stub {
            on(gps.hasValidReading).thenReturn(false)
            on(gps.location).thenReturn(Coordinate.zero)
            onBlocking { read() }.doReturn(Unit)
        }
        val strategy = NearestTideSelectionStrategy(gps)
        val tides = listOf(
            table(1, 100, Coordinate(1.0, 1.0)),
            table(2, 100, Coordinate(0.1, 1.0)),
            table(3, 100, Coordinate(2.0, 1.0)),
            table(4, 100, null),
        )

        Assertions.assertEquals(tides[1], strategy.getTide(tides))
        verify(gps, atLeastOnce()).read()
    }

    @Test
    fun getTideNoReadGPS() = runBlocking {
        val gps = mock<IGPS>()
        gps.stub {
            on(gps.hasValidReading).thenReturn(true)
            on(gps.location).thenReturn(Coordinate.zero)
            onBlocking { read() }.doReturn(Unit)
        }
        val strategy = NearestTideSelectionStrategy(gps)
        val tides = listOf(
            table(1, 100, Coordinate(1.0, 1.0)),
            table(2, 100, Coordinate(0.1, 1.0)),
            table(3, 100, Coordinate(2.0, 1.0)),
            table(4, 100, null),
        )

        Assertions.assertEquals(tides[1], strategy.getTide(tides))
        verify(gps, never()).read()
    }

    @Test
    fun getTideNoCoords() = runBlocking {
        val gps = mock<IGPS>()
        gps.stub {
            on(gps.hasValidReading).thenReturn(false)
            on(gps.location).thenReturn(Coordinate.zero)
            onBlocking { read() }.doReturn(Unit)
        }
        val strategy = NearestTideSelectionStrategy(gps)
        val tides = listOf(
            table(4, 10, null)
        )

        verify(gps, never()).read()
        Assertions.assertNull(strategy.getTide(tides))
    }

    @Test
    fun getTideOneLocation() = runBlocking {
        val gps = mock<IGPS>()
        gps.stub {
            on(gps.hasValidReading).thenReturn(false)
            on(gps.location).thenReturn(Coordinate.zero)
            onBlocking { read() }.doReturn(Unit)
        }
        val strategy = NearestTideSelectionStrategy(gps)
        val tides = listOf(
            table(1, 100, Coordinate(1.0, 1.0))
        )

        verify(gps, never()).read()
        Assertions.assertEquals(tides[0], strategy.getTide(tides))
    }

    private fun table(id: Long, millis: Long, location: Coordinate?): TideTable {
        return TideTable(id, listOf(Tide.high(Instant.ofEpochMilli(millis).utc())), null, location)
    }
}