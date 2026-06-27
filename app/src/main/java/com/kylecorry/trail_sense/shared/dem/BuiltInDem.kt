package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.luna.cache.MemoryCachedValue
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.shared.io.FileSubsystem

object BuiltInDem {

    private val cache = MemoryCachedValue<List<DigitalElevationModelEntity>>()

    suspend fun getTiles(): List<DigitalElevationModelEntity> = onDefault {
        cache.getOrPut {
            val files = DependencyRegistry.get<FileSubsystem>()
            val indexText = files.readAsset("dem/index.json") ?: ""
            DigitalElevationModelLoader.getTilesFromIndex(indexText, true)
        }
    }
}
