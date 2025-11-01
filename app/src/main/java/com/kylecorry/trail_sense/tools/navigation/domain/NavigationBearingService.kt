package com.kylecorry.trail_sense.tools.navigation.domain

import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.main.persistence.ICleanable
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingDao
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

class NavigationBearingService(private val dao: NavigationBearingDao) : ICleanable {

    private val mutex = Mutex()

    suspend fun setBearing(bearing: NavigationBearing?): Unit = mutex.withLock {
        onIO {
            dao.clearActiveBearing()
            if (bearing != null) {
                val entity = NavigationBearingEntity.from(bearing.copy(isActive = true))
                dao.upsert(entity)
            }
        }
    }

    fun getBearing(): Flow<NavigationBearing?> {
        return dao.getActiveBearingFlow().map { it?.toNavigationBearing() }
    }

    suspend fun isNavigating(): Boolean {
        return dao.isNavigating()
    }

    override suspend fun clean() {
        dao.deleteOlderThan(Instant.now().minus(BEARING_HISTORY_DURATION).toEpochMilli())
    }

    companion object {
        private var instance: NavigationBearingService? = null
        private var BEARING_HISTORY_DURATION = Duration.ofHours(12)

        @Synchronized
        fun getInstance(context: Context): NavigationBearingService {
            if (instance == null) {
                instance = NavigationBearingService(AppDatabase.getInstance(context).bearingDao())
            }
            return instance!!
        }
    }

}