package com.kylecorry.trail_sense.tools.tides.domain

interface ITideSelectionStrategy {
    suspend fun getTide(tides: List<TideEntity>): TideEntity?
}