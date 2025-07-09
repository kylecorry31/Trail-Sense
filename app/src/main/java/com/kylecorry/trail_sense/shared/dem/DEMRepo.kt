package com.kylecorry.trail_sense.shared.dem

import android.util.Log
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.main.persistence.ICleanable
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DEMRepo private constructor() : ICleanable {

    private val files = AppServiceRegistry.get<FileSubsystem>()
    private val database = AppServiceRegistry.get<AppDatabase>()
    private val prefs = AppServiceRegistry.get<UserPreferences>()

    override suspend fun clean() {
        var removed = false
        lock.withLock {
            val expectedVersion = database.digitalElevationModelDao().getVersion()
            val versionFile = files.get("dem/version.txt")
            if (!versionFile.exists() || versionFile.readText().trim() != expectedVersion) {
                Log.d("DEMRepo", "DEM version mismatch")
                database.digitalElevationModelDao().deleteAll()
                files.getDirectory("dem").deleteRecursively()
                prefs.altimeter.isDigitalElevationModelLoaded = false
                removed = true
            }
        }

        if (removed) {
            DEM.invalidateCache()
        }
    }

    suspend fun getVersion(): String? = onDefault {
        lock.withLock {
            database.digitalElevationModelDao().getVersion()
        }
    }

    companion object {
        private var instance: DEMRepo? = null
        val lock = Mutex()

        @Synchronized
        fun getInstance(): DEMRepo {
            if (instance == null) {
                instance = DEMRepo()
            }
            return instance!!
        }
    }
}