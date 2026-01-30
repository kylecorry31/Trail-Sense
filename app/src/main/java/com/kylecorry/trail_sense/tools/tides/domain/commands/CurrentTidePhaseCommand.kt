package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class CurrentTidePhaseCommand(
    private val tideService: TideService,
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    suspend fun execute(table: TideTable): Float? = onDefault {
        val now = timeProvider.getTime()
        tideService.getPhase(table, now)
    }

}