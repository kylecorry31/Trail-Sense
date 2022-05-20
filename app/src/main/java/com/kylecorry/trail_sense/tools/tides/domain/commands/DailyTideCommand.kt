package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.tools.tides.domain.ITideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.DailyTideData
import java.time.LocalDate

class DailyTideCommand(private val tideService: ITideService) {

    suspend fun execute(table: TideTable, date: LocalDate): DailyTideData = onDefault {
        val levels = tideService.getWaterLevels(table, date)
        val tides = tideService.getTides(table, date)
        val range = tideService.getRange(table)
        DailyTideData(levels, tides, range)
    }

}