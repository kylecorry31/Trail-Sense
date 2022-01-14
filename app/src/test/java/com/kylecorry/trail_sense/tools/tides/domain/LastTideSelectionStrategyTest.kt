package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.time.Time.utc
import com.kylecorry.trail_sense.settings.infrastructure.ITidePreferences
import com.kylecorry.trail_sense.tools.tides.domain.selection.LastTideSelectionStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

internal class LastTideSelectionStrategyTest {

    @Test
    fun getTideWithLastTide() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = LastTideSelectionStrategy(prefs)
        val tides = listOf(
            table(2, 100),
            table(1, 10)
        )

        assertEquals(tides[1], strategy.getTide(tides))
        verify(prefs, never()).lastTide = null
    }

    @Test
    fun getTideWithLastTideAndClear() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = LastTideSelectionStrategy(prefs, true)
        val tides = listOf(
            table(2, 100),
            table(1, 10)
        )

        assertEquals(tides[1], strategy.getTide(tides))
        verify(prefs, atLeastOnce()).lastTide = null
    }

    @Test
    fun getTideWithoutLastTide() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(null)
        val strategy = LastTideSelectionStrategy(prefs)
        val tides = listOf(
            table(2, 100),
            table(1, 10)
        )

        assertNull(strategy.getTide(tides))
    }

    @Test
    fun getTideWithNonExistingLastTide() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(3L)
        val strategy = LastTideSelectionStrategy(prefs)
        val tides = listOf(
            table(2, 100),
            table(1, 10)
        )

        assertNull(strategy.getTide(tides))
    }

    private fun table(id: Long, millis: Long): TideTable {
        return TideTable(id, listOf(Tide.high(Instant.ofEpochMilli(millis).utc())), null, null)
    }
}