package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class FieldGuideCleanupCommand(context: Context) : CoroutineCommand {

    private val repo = FieldGuideRepo.getInstance(context)
    private val files = FileSubsystem.getInstance(context)


    override suspend fun execute() = onIO {
        val pages = repo.getAllPages().filter { !it.isReadOnly }
        val allFiles = files.list("field_guide").map { "field_guide/${it.name}" }

        // Delete files without a page
        val existingFiles = pages.flatMap { it.images }
        val orphanedFiles = allFiles.filter { !existingFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }
    }
}