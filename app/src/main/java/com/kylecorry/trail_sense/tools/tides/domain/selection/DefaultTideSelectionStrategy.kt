package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class DefaultTideSelectionStrategy: ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? {
        return tides.firstOrNull()
    }
}