package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity

class TideRepo private constructor(context: Context) : ITideRepo {

    private val tideDao = AppDatabase.getInstance(context).tideDao()

    override fun getTides() = tideDao.getAll()

    override suspend fun getTide(id: Long) = tideDao.get(id)

    override suspend fun deleteTide(tide: TideEntity) = tideDao.delete(tide)

    override suspend fun addTide(tide: TideEntity) {
        if (tide.id != 0L){
            tideDao.update(tide)
        } else {
            tideDao.insert(tide)
        }
    }

    companion object {
        private var instance: TideRepo? = null

        @Synchronized
        fun getInstance(context: Context): TideRepo {
            if (instance == null) {
                instance = TideRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}