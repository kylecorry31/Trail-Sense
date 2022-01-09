package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NearestTideSelectionStrategy(
    private val gps: IGPS
) : ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? =
        withContext(Dispatchers.IO) {
            val tidesWithLocation = tides.filter { it.location != null }
            if (tidesWithLocation.size <= 1){
                return@withContext tidesWithLocation.firstOrNull()
            }
            if (!gps.hasValidReading) {
                gps.read()
            }
            tidesWithLocation.minByOrNull { it.location!!.distanceTo(gps.location) }
        }
}