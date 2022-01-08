package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.andromeda.location.IGPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NearestTideSelectionStrategy(
    private val gps: IGPS
) : ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideEntity>): TideEntity? =
        withContext(Dispatchers.IO) {
            val tidesWithLocation = tides.filter { it.coordinate != null }
            if (tidesWithLocation.size <= 1){
                return@withContext tidesWithLocation.firstOrNull()
            }
            if (!gps.hasValidReading) {
                gps.read()
            }
            tidesWithLocation.minByOrNull { it.coordinate!!.distanceTo(gps.location) }
        }
}