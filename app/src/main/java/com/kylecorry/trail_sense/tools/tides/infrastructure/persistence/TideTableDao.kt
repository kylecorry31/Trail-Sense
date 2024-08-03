package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TideTableDao {

    // Tide Tables

    @Query("SELECT * FROM tide_tables")
    suspend fun getTideTables(): List<TideTableEntity>

    @Query("SELECT * FROM tide_tables")
    fun getTideTablesLive(): LiveData<List<TideTableEntity>>

    @Query("SELECT * FROM tide_tables WHERE _id = :id")
    suspend fun getTideTable(id: Long): TideTableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(table: TideTableEntity): Long

    @Update
    suspend fun update(table: TideTableEntity)

    @Delete
    suspend fun delete(table: TideTableEntity)

    // Tide Table Rows

    @Query("SELECT * FROM tide_table_rows WHERE table_id = :tableId")
    suspend fun getTideTableRows(tableId: Long): List<TideTableRowEntity>

    @Delete
    suspend fun delete(row: TideTableRowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: TideTableRowEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(rows: List<TideTableRowEntity>)

    @Query("DELETE FROM tide_table_rows WHERE table_id = :tableId")
    suspend fun deleteRowsForTable(tableId: Long)

    // Tide constituents

    @Query("SELECT * FROM tide_constituents WHERE table_id = :tableId")
    suspend fun getTideConstituents(tableId: Long): List<TideConstituentEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(constituent: TideConstituentEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsertConstituents(constituents: List<TideConstituentEntry>)

    @Delete
    suspend fun delete(constituent: TideConstituentEntry)

    @Query("DELETE FROM tide_constituents WHERE table_id = :tableId")
    suspend fun deleteConstituentsForTable(tableId: Long)
}