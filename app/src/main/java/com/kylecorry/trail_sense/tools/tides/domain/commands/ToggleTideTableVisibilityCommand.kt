package com.kylecorry.trail_sense.tools.tides.domain.commands

import android.content.Context
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo

class ToggleTideTableVisibilityCommand(context: Context) {

    private val repo = TideTableRepo.getInstance(context)

    suspend fun execute(table: TideTable): TideTable {
        val newTable = table.copy(isVisible = !table.isVisible)
        repo.addTideTable(newTable)
        return newTable
    }

}