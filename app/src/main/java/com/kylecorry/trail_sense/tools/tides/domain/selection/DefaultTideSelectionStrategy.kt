package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideEntity

class DefaultTideSelectionStrategy: ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideEntity>): TideEntity? {
        return tides.firstOrNull()
    }
}