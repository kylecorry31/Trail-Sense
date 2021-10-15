package com.kylecorry.trail_sense.shared.database

import androidx.lifecycle.LiveData
import com.kylecorry.sol.units.Reading

interface IReadingRepo<T : Identifiable>: ICleanable {

    suspend fun add(reading: Reading<T>): Long

    suspend fun delete(reading: Reading<T>)

    suspend fun get(id: Long): Reading<T>?

    suspend fun getAll(): List<Reading<T>>

    fun getAllLive(): LiveData<List<Reading<T>>>
}