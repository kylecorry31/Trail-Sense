package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.tools.tides.domain.ITideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData

class CurrentTideCommand(
    private val tideService: ITideService,
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