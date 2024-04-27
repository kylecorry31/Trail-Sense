package com.kylecorry.trail_sense.tools.tides.domain.selection

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class NearestTideSelectionStrategy(
    private val locationProvider: () -> Coordinate
) : ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? = onIO {
        val tidesWithLocation = tides.filter { it.location != null }
        if (tidesWithLocation.size <= 1) {
            return@onIO tidesWithLocation.firstOrNull()
        }
        tidesWithLocation.minByOrNull { it.location!!.distanceTo(locationProvider()) }
    }
}