package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import java.time.Instant

class BatteryRepo private constructor(context: Context) : IBatteryRepo {

    private val batteryDao = AppDatabase.getInstance(context).batteryDao()

    override fun get(): LiveData<List<BatteryReadingEntity>> {
        return batteryDao.get()
    }

    override suspend fun add(reading: BatteryReadingEntity) {
        batteryDao.insert(reading)
    }

    override suspend fun deleteBefore(time: Instant) {
        batteryDao.deleteOlderThan(time)
    }

    companion object {
        private var instance: BatteryRepo? = null

        @Synchronized
        fun getInstance(context: Context): BatteryRepo {
            if (instance == null) {
                instance = BatteryRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}