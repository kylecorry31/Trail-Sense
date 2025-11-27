package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.io.InputStream

class LocalInputStreamable(private val assetPath: String) : InputStreamable {
    override suspend fun getInputStream(): InputStream? {
        val files = AppServiceRegistry.get<FileSubsystem>()
        return files.get(assetPath).inputStream()
    }
}