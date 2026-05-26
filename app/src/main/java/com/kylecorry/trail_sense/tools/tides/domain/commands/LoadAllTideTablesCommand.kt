package com.kylecorry.trail_sense.tools.tides.domain.commands

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo

class LoadAllTideTablesCommand(context: Context) {

    private val repo = TideTableRepo.getInstance(context)

    suspend fun execute(): List<TideTable> = onIO {
        repo.getTideTables()
    }

}
