package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

interface ITideTableRepo {
    suspend fun getTideTables(): List<TideTable>

    suspend fun getTideTable(id: Long): TideTable?

    suspend fun addTideTable(table: TideTable): Long

    suspend fun deleteTideTable(table: TideTable)

    suspend fun addTides(tableId: Long, tides: List<Tide>)

    suspend fun deleteTides(tableId: Long)
}