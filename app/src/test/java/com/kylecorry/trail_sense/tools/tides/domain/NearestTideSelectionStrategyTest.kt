package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.units.Coordinate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

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
            TideEntity(100, null, 1.0, 1.0).also {
                it.id = 1
            },
            TideEntity(10, null, 0.1, 1.0).also {
                it.id = 2
            },
            TideEntity(10, null, 2.0, 1.0).also {
                it.id = 3
            },
            TideEntity(10, null, null, null).also {
                it.id = 4
            }
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
            TideEntity(100, null, 1.0, 1.0).also {
                it.id = 1
            },
            TideEntity(10, null, 0.1, 1.0).also {
                it.id = 2
            },
            TideEntity(10, null, 2.0, 1.0).also {
                it.id = 3
            },
            TideEntity(10, null, null, null).also {
                it.id = 4
            }
        )

        Assertions.assertEquals(tides[1], strategy.getTide(tides))
        verify(gps, never()).read()
    }

    @Test
    fun getTideNoCoords() = runBlocking {
        val gps = mock<IGPS>()
        gps.stub {
            on(gps.hasValidReading).thenReturn(true)
            on(gps.location).thenReturn(Coordinate.zero)
            onBlocking { read() }.doReturn(Unit)
        }
        val strategy = NearestTideSelectionStrategy(gps)
        val tides = listOf(
            TideEntity(10, null, null, null).also {
                it.id = 4
            }
        )

        Assertions.assertNull(strategy.getTide(tides))
    }
}