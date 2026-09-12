package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.andromeda.core.time.IZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData

class CurrentTideCommand(
    private val tideService: TideService,
    private val timeProvider: IZonedDateTimeProvider = SystemZonedDateTimeProvider()
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
