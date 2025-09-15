package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.io.InputStream

class AssetInputStreamable(private val assetPath: String) : InputStreamable {
    override suspend fun getInputStream(): InputStream? {
        val files = AppServiceRegistry.get<FileSubsystem>()
        return files.streamAsset(assetPath)
    }
}