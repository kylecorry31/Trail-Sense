package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import android.content.Context
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class TideTableRepo private constructor(private val dao: TideTableDao) : ITideTableRepo {

    override suspend fun getTideTables(): List<TideTable> {
        val tableEntities = dao.getTideTables()
        val tables = mutableListOf<TideTable>()

        for (entity in tableEntities) {
            val rows = dao.getTideTableRows(entity.id).map { it.toTide() }.sortedBy { it.time }
            tables.add(entity.toTable(rows))
        }

        return tables
    }

    override suspend fun getTideTable(id: Long): TideTable? {
        val rows = dao.getTideTableRows(id).map { it.toTide() }.sortedBy { it.time }
        return dao.getTideTable(id)?.toTable(rows)
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
                instance = TideTableRepo(AppDatabase.getInstance(context).tideTableDao())
            }
            return instance!!
        }
    }

}