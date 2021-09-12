package com.kylecorry.trail_sense.tools.tides.domain

class DefaultTideSelectionStrategy: ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideEntity>): TideEntity? {
        return tides.firstOrNull()
    }
}