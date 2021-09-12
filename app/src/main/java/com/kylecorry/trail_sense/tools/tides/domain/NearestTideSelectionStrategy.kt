package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.andromeda.location.IGPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NearestTideSelectionStrategy(
    private val gps: IGPS
) : ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideEntity>): TideEntity? =
        withContext(Dispatchers.IO) {
            if (!gps.hasValidReading) {
                gps.read()
            }
            tides.filter { it.coordinate != null }
                .minByOrNull { it.coordinate!!.distanceTo(gps.location) }
        }
}