package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.io.InputStream

class AssetInputStreamable(private val assetPath: String) : InputStreamable {
    override suspend fun getInputStream(): InputStream? {
        val files = DependencyRegistry.get<FileSubsystem>()
        return files.streamAsset(assetPath)
    }
}
