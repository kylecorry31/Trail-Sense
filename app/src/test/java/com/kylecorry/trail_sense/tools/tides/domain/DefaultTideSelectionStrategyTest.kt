package com.kylecorry.trail_sense.tools.tides.domain

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class DefaultTideSelectionStrategyTest {

    @Test
    fun getTide() = runBlocking {
        val strategy = DefaultTideSelectionStrategy()
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
    fun getTideEmptyList() = runBlocking {
        val strategy = DefaultTideSelectionStrategy()
        val tides = emptyList<TideEntity>()
        Assertions.assertNull(strategy.getTide(tides))
    }
}