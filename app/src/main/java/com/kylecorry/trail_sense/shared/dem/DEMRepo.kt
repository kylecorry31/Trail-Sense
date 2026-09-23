package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.main.persistence.ICleanable
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DEMRepo private constructor() : ICleanable {

    private val files = DependencyRegistry.get<FileSubsystem>()
    private val database = DependencyRegistry.get<AppDatabase>()
    private val prefs = DependencyRegistry.get<UserPreferences>()

    override suspend fun clean() {
        var removed = false
        lock.withLock {
            val expectedVersion = database.digitalElevationModelDao().getVersion()
            val versionFile = files.get("dem/version.txt")
            val actualVersion = if (versionFile.exists()) versionFile.readText().trim() else null
            if (actualVersion != expectedVersion) {
                database.digitalElevationModelDao().deleteAll()
                if (files.getDirectory("dem").exists()) {
                    getAppService<Logger>().info(
                        TAG,
                        "DEM version mismatch (expected: $expectedVersion, found: $actualVersion), removing DEM files"
                    )
                    files.getDirectory("dem").deleteRecursively()
                }
                prefs.altimeter.isDigitalElevationModelLoaded = false
                removed = true
            }

            if (actualVersion == null && prefs.altimeter.isDigitalElevationModelLoaded) {
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
        private const val TAG = "DEMRepo"
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
