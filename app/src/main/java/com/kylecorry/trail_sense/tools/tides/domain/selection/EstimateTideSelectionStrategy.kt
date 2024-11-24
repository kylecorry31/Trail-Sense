package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

class EstimateTideSelectionStrategy : ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? {
        return tides.firstOrNull { it.estimator == TideEstimator.TideModel && it.location == null }
    }
}