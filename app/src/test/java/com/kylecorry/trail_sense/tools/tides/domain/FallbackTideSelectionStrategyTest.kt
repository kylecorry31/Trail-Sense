package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.trail_sense.settings.infrastructure.ITidePreferences
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class FallbackTideSelectionStrategyTest {

    @Test
    fun getTideFirst() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = FallbackTideSelectionStrategy(LastTideSelectionStrategy(prefs), DefaultTideSelectionStrategy())
        val tides = listOf(
            TideEntity(100, null, null, null).also {
                it.id = 2
            },
            TideEntity(10, null, null, null).also {
                it.id = 1
            }
        )

        Assertions.assertEquals(tides[1], strategy.getTide(tides))
    }

    @Test
    fun getTideSecond() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(null)
        val strategy = FallbackTideSelectionStrategy(LastTideSelectionStrategy(prefs), DefaultTideSelectionStrategy())
        val tides = listOf(
            TideEntity(100, null, null, null).also {
                it.id = 2
            },
            TideEntity(10, null, null, null).also {
                it.id = 1
            }
        )

        Assertions.assertEquals(tides[0], strategy.getTide(tides))
    }

    @Test
    fun getTideNone() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = FallbackTideSelectionStrategy(LastTideSelectionStrategy(prefs), DefaultTideSelectionStrategy())
        val tides = emptyList<TideEntity>()

        Assertions.assertNull(strategy.getTide(tides))
    }
}