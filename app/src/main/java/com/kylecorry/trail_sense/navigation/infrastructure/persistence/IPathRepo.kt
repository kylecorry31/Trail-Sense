package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.paths.Path2

interface IPathRepo {
    suspend fun add(value: Path2): Long

    suspend fun delete(value: Path2)

    suspend fun get(id: Long): Path2?

    suspend fun getAll(): List<Path2>

    fun getAllLive(): LiveData<List<Path2>>
}