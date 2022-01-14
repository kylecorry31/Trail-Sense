package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.trail_sense.tools.tides.domain.TideTable

interface ITideSelectionStrategy {
    suspend fun getTide(tides: List<TideTable>): TideTable?
}