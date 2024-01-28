package com.kylecorry.trail_sense.main.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.Identifiable

interface IReadingRepo<T : Identifiable> : ICleanable {

    suspend fun add(reading: Reading<T>): Long

    suspend fun delete(reading: Reading<T>)

    suspend fun get(id: Long): Reading<T>?

    suspend fun getAll(): List<Reading<T>>

    fun getAllLive(): LiveData<List<Reading<T>>>
}