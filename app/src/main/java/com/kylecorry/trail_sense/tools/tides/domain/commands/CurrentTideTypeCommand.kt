package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.tools.tides.domain.ITideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class CurrentTideTypeCommand(
    private val tideService: ITideService,
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    suspend fun execute(table: TideTable): TideType? = onDefault {
        val now = timeProvider.getTime()
        tideService.getCurrentTide(table, now)
    }

}