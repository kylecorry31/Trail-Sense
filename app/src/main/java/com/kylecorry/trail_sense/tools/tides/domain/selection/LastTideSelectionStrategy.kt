package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.trail_sense.settings.infrastructure.ITidePreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LastTideSelectionStrategy(
    private val prefs: ITidePreferences,
    private val clearLastTideAfterUse: Boolean = false
) :
    ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? =
        withContext(Dispatchers.IO) {
            val lastTide = prefs.lastTide
            if (clearLastTideAfterUse) {
                prefs.lastTide = null
            }
            tides.firstOrNull { tide -> tide.id == lastTide }
        }
}