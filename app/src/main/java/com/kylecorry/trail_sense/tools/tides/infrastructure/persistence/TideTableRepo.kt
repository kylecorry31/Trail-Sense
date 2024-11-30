package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

class TideTableRepo private constructor(
    private val dao: TideTableDao,
    private val context: Context
) : ITideTableRepo {

    override suspend fun getTideTables(): List<TideTable> = onIO {
        val tableEntities = dao.getTideTables()
        val tables = mutableListOf<TideTable>()

        for (entity in tableEntities) {
            val rows = dao.getTideTableRows(entity.id).map { it.toTide() }.sortedBy { it.time }
            val harmonics = dao.getTideConstituents(entity.id).map { it.toHarmonic() }
            tables.add(entity.toTable(rows, harmonics))
        }

        listOf(
            TideTable(
                -1,
                emptyList(),
                context.getString(R.string.navigation_nearby_category),
                estimator = TideEstimator.TideModel,
                isEditable = false
            )
        ) + tables
    }

    override suspend fun getTideTable(id: Long): TideTable? {
        val rows = dao.getTideTableRows(id).map { it.toTide() }.sortedBy { it.time }
        val harmonics = dao.getTideConstituents(id).map { it.toHarmonic() }
        return dao.getTideTable(id)?.toTable(rows, harmonics)
    }

    override suspend fun addTideTable(table: TideTable): Long {
        return if (table.id == 0L) {
            val newId = dao.insert(TideTableEntity.from(table))
            addTides(newId, table.tides)
            newId
        } else {
            // TODO: Only delete the tides if they changed
            deleteTides(table.id)
            addTides(table.id, table.tides)
            dao.update(TideTableEntity.from(table))
            table.id
        }
    }

    override suspend fun deleteTideTable(table: TideTable) {
        deleteTides(table.id)
        dao.delete(TideTableEntity.from(table))
    }

    override suspend fun addTides(tableId: Long, tides: List<Tide>) {
        dao.bulkInsert(tides.map { TideTableRowEntity.from(0, tableId, it) })
    }

    override suspend fun deleteTides(tableId: Long) {
        dao.deleteRowsForTable(tableId)
    }

    companion object {
        private var instance: TideTableRepo? = null

        @Synchronized
        fun getInstance(context: Context): TideTableRepo {
            if (instance == null) {
                instance = TideTableRepo(
                    AppDatabase.getInstance(context).tideTableDao(),
                    context.applicationContext
                )
            }
            return instance!!
        }
    }

}