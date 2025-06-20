package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.cache.MemoryCachedValue
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.shared.io.FileSubsystem

object BuiltInDem {

    private val cache = MemoryCachedValue<List<DigitalElevationModelEntity>>()

    suspend fun getTiles(): List<DigitalElevationModelEntity> = onDefault {
        cache.getOrPut {
            val files = AppServiceRegistry.get<FileSubsystem>()
            val indexText = files.readAsset("dem/index.json") ?: ""
            DigitalElevationModelLoader.getTilesFromIndex(indexText)
        }
    }
}