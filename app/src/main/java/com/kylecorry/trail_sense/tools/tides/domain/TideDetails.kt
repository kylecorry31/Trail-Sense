package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData
import com.kylecorry.trail_sense.tools.tides.ui.DailyTideData

data class TideDetails(
    val table: TideTable,
    val now: CurrentTideData,
    val today: DailyTideData
)