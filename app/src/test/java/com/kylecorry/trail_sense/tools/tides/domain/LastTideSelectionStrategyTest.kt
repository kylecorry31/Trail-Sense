package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.trail_sense.settings.infrastructure.ITidePreferences
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

internal class LastTideSelectionStrategyTest {

    @Test
    fun getTideWithLastTide() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = LastTideSelectionStrategy(prefs)
        val tides = listOf(
            TideEntity(100, null, null, null).also {
                it.id = 2
            },
            TideEntity(10, null, null, null).also {
                it.id = 1
            }
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
            TideEntity(100, null, null, null).also {
                it.id = 2
            },
            TideEntity(10, null, null, null).also {
                it.id = 1
            }
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
            TideEntity(100, null, null, null).also {
                it.id = 2
            },
            TideEntity(10, null, null, null).also {
                it.id = 1
            }
        )

        assertNull(strategy.getTide(tides))
    }

    @Test
    fun getTideWithNonExistingLastTide() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(3L)
        val strategy = LastTideSelectionStrategy(prefs)
        val tides = listOf(
            TideEntity(100, null, null, null).also {
                it.id = 2
            },
            TideEntity(10, null, null, null).also {
                it.id = 1
            }
        )

        assertNull(strategy.getTide(tides))
    }
}