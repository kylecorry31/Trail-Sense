package com.kylecorry.trail_sense.weather.infrastructure.database

import android.content.Context
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity
import java.time.Instant

class PressureRepo private constructor(context: Context) : IPressureRepo {

    private val pressureDao = AppDatabase.getInstance(context).pressureDao()

    override fun getPressures() = pressureDao.getAll()

    override suspend fun getPressuresSync() = pressureDao.getAllSync()

    override suspend fun getPressure(id: Long) = pressureDao.get(id)

    override suspend fun deletePressure(pressure: PressureReadingEntity) = pressureDao.delete(pressure)

    override suspend fun deleteOlderThan(instant: Instant) = pressureDao.deleteOlderThan(instant.toEpochMilli())

    override suspend fun addPressure(pressure: PressureReadingEntity) {
        if (pressure.id != 0L){
            pressureDao.update(pressure)
        } else {
            pressureDao.insert(pressure)
        }
    }

    companion object {
        private var instance: PressureRepo? = null

        @Synchronized
        fun getInstance(context: Context): PressureRepo {
            if (instance == null) {
                instance = PressureRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}