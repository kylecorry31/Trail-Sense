package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import java.time.Instant

class CurrentTidePhaseCommand(
    private val tideService: TideService,
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    suspend fun execute(table: TideTable, time: Instant? = null): Float? = onDefault {
        val now = time?.toZonedDateTime() ?: timeProvider.getTime()
        tideService.getPhase(table, now)
    }

}