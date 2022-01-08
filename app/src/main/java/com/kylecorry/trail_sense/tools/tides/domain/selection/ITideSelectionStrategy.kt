package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideEntity

interface ITideSelectionStrategy {
    suspend fun getTide(tides: List<TideEntity>): TideEntity?
}