package com.kylecorry.trail_sense.tools.notes.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.tools.notes.domain.Note

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAll(): LiveData<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAllSync(): List<Note>

    @Query("SELECT * FROM notes WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(note: Note)
}