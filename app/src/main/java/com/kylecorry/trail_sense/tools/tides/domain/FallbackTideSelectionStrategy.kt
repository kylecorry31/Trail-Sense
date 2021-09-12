package com.kylecorry.trail_sense.tools.tides.domain

class FallbackTideSelectionStrategy(private vararg val strategies: ITideSelectionStrategy) :
    ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideEntity>): TideEntity? {
        for (strategy in strategies) {
            val tide = strategy.getTide(tides)
            if (tide != null) {
                return tide
            }
        }
        return null
    }
}