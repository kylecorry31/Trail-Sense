package com.kylecorry.trail_sense.tools.notes.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.tools.notes.domain.Note

class NoteRepo private constructor(context: Context) : INoteRepo {

    private val noteDao = AppDatabase.getInstance(context.applicationContext).noteDao()

    override fun getNotes() = noteDao.getAll()

    override suspend fun getNote(id: Long) = noteDao.get(id)

    override suspend fun deleteNote(note: Note) = noteDao.delete(note)

    override suspend fun addNote(note: Note) {
        if (note.id != 0L){
            noteDao.update(note)
        } else {
            noteDao.insert(note)
        }
    }

    companion object {
        private var instance: NoteRepo? = null

        @Synchronized
        fun getInstance(context: Context): NoteRepo {
            if (instance == null) {
                instance = NoteRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}