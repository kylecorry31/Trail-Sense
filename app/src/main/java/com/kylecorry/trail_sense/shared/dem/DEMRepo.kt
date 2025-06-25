package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.main.persistence.ICleanable
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class DEMRepo private constructor() : ICleanable {

    private val files = AppServiceRegistry.get<FileSubsystem>()
    private val database = AppServiceRegistry.get<AppDatabase>()

    override suspend fun clean() {
        val tiles = database.digitalElevationModelDao().getAll()
        // Delete orphaned tiles
        for (tile in tiles) {
            val file = files.get(tile.filename)
            if (!file.exists()) {
                database.digitalElevationModelDao().delete(tile)
            }
        }
    }

    companion object {
        private var instance: DEMRepo? = null

        @Synchronized
        fun getInstance(): DEMRepo {
            if (instance == null) {
                instance = DEMRepo()
            }
            return instance!!
        }
    }
}