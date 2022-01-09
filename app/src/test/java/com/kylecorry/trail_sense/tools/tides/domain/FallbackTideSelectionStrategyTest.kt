package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.time.Time.utc
import com.kylecorry.trail_sense.settings.infrastructure.ITidePreferences
import com.kylecorry.trail_sense.tools.tides.domain.selection.DefaultTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.FallbackTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.LastTideSelectionStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

internal class FallbackTideSelectionStrategyTest {

    @Test
    fun getTideFirst() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = FallbackTideSelectionStrategy(LastTideSelectionStrategy(prefs), DefaultTideSelectionStrategy())
        val tides = listOf(
            table(2, 100),
            table(1, 10)
        )

        Assertions.assertEquals(tides[1], strategy.getTide(tides))
    }

    @Test
    fun getTideSecond() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(null)
        val strategy = FallbackTideSelectionStrategy(LastTideSelectionStrategy(prefs), DefaultTideSelectionStrategy())
        val tides = listOf(
            table(2, 100),
            table(1, 10)
        )

        Assertions.assertEquals(tides[0], strategy.getTide(tides))
    }

    @Test
    fun getTideNone() = runBlocking {
        val prefs = mock<ITidePreferences>()
        whenever(prefs.lastTide).thenReturn(1L)
        val strategy = FallbackTideSelectionStrategy(LastTideSelectionStrategy(prefs), DefaultTideSelectionStrategy())
        val tides = emptyList<TideTable>()

        Assertions.assertNull(strategy.getTide(tides))
    }

    private fun table(id: Long, millis: Long): TideTable {
        return TideTable(id, listOf(Tide.high(Instant.ofEpochMilli(millis).utc())), null, null)
    }

}