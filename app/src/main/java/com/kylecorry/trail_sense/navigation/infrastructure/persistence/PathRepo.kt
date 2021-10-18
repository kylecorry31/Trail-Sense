package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.paths.Path2

class PathRepo(context: Context): IPathRepo {
    override suspend fun add(value: Path2): Long {
        TODO("Not yet implemented")
    }

    override suspend fun delete(value: Path2) {
        TODO("Not yet implemented")
    }

    override suspend fun get(id: Long): Path2? {
        TODO("Not yet implemented")
    }

    override suspend fun getAll(): List<Path2> {
        TODO("Not yet implemented")
    }

    override fun getAllLive(): LiveData<List<Path2>> {
        TODO("Not yet implemented")
    }
}