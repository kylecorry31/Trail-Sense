package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import android.content.Context
import android.net.Uri
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest

class CreateMapFromUriCommand(
    private val context: Context,
    private val uri: Uri
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun execute(): CreateOfflineMapRequest? = onIO {
        val filename = files.getFileName(uri, withExtension = false, fallbackToPathName = false)
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.name),
                hint = context.getString(R.string.name),
                default = filename
            )
        } ?: return@onIO null

        CreateOfflineMapRequest(uri, name)
    }
}
