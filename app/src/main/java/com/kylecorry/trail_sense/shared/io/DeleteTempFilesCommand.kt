package com.kylecorry.trail_sense.shared.io

import android.content.Context
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onIO

class DeleteTempFilesCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() = onIO {
        val dir = LocalFiles.getDirectory(context, Files.TEMP_DIR, false)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
}