package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.tools.tides.domain.ITideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import com.kylecorry.trail_sense.tools.tides.ui.DailyTideData
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

class DailyTideCommand(private val tideService: ITideService) {

    suspend fun execute(table: TideTable, date: LocalDate): DailyTideData = onDefault {
        val levels = tideService.getWaterLevels(table, date)
        val tides = tideService.getTides(table, date)
        val range = tideService.getRange(table)

        val actualRange = levels.map { it.value }.range()

        val finalRange = if (table.estimator == TideEstimator.Harmonic && table.tides.isEmpty()) {
            actualRange ?: range
        } else {
            Range(
                min(actualRange?.start ?: range.start, range.start),
                max(actualRange?.end ?: range.end, range.end)
            )
        }

        DailyTideData(levels, tides, finalRange)
    }

}