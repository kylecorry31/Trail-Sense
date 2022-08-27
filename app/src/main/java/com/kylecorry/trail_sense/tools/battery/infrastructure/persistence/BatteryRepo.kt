package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import java.time.Instant

class BatteryRepo private constructor(context: Context) : IBatteryRepo {

    private val batteryDao = AppDatabase.getInstance(context).batteryDao()
    private val prefs = Preferences(context)
    private var maxCapacityPref by FloatPreference(prefs, "pref_max_battery_capacity", 0f)

    override fun get(): LiveData<List<BatteryReadingEntity>> {
        return batteryDao.get()
    }

    override suspend fun add(reading: BatteryReadingEntity) {
        onIO {
            batteryDao.insert(reading)

            // Record the maximum capacity the battery has reached
            if (getMaxCapacity() < reading.capacity) {
                maxCapacityPref = reading.capacity
            }
        }
    }

    override suspend fun deleteBefore(time: Instant) = onIO {
        batteryDao.deleteOlderThan(time)
    }

    override fun getMaxCapacity(): Float {
        return maxCapacityPref
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