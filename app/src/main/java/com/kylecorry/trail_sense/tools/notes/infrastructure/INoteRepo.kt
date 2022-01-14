package com.kylecorry.trail_sense.tools.notes.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.notes.domain.Note

interface INoteRepo {
    fun getNotes(): LiveData<List<Note>>

    suspend fun getNotesSync(): List<Note>

    suspend fun getNote(id: Long): Note?

    suspend fun deleteNote(note: Note)

    suspend fun addNote(note: Note): Long
}