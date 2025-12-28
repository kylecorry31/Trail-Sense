package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData

class CurrentTideCommand(
    private val tideService: TideService,
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    suspend fun execute(table: TideTable): CurrentTideData = onDefault {
        val now = timeProvider.getTime()
        val level = tideService.getWaterLevel(table, now)
        val isRising = tideService.isRising(table, now)
        val type = tideService.getCurrentTide(table, now)
        val withinTable = tideService.isWithinTideTable(table, now)
        CurrentTideData(if (withinTable) level else null, type, isRising)
    }

}