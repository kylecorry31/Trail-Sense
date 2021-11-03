package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.paths.Path

class PathRepo private constructor(context: Context) : IPathRepo {

    private val pathDao = AppDatabase.getInstance(context).pathDao()

    override suspend fun add(value: Path): Long {
        return if (value.id != 0L) {
            pathDao.update(PathEntity.from(value))
            value.id
        } else {
            pathDao.insert(PathEntity.from(value))
        }
    }

    override suspend fun delete(value: Path) {
        pathDao.delete(PathEntity.from(value))
    }

    override suspend fun get(id: Long): Path? {
        return pathDao.get(id)?.toPath()
    }

    override fun getLive(id: Long): LiveData<Path?> {
        return Transformations.map(pathDao.getLive(id)){ it?.toPath() }
    }

    override suspend fun getAll(): List<Path> {
        return pathDao.getAllSuspend().map { it.toPath() }
    }

    override fun getAllLive(): LiveData<List<Path>> {
        return Transformations.map(pathDao.getAll()) {
            it.map { path -> path.toPath() }
        }
    }

    companion object {
        private var instance: PathRepo? = null

        @Synchronized
        fun getInstance(context: Context): PathRepo {
            if (instance == null) {
                instance = PathRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}