package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.create

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.IMapRepo

class CreateMapFromUriCommand(
    private val context: Context,
    private val repo: IMapRepo,
    private val uri: Uri,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun execute(): PhotoMap? = onIO {
        val filename = files.getFileName(uri, withExtension = false, fallbackToPathName = false)
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.name),
                hint = context.getString(R.string.name),
                default = filename
            )
        } ?: return@onIO null

        try {
            onMain {
                loadingIndicator.show()
            }
            val type = files.getMimeType(uri)
            if (type == "application/pdf") {
                CreateMapFromPDFCommand(context, repo, name).execute(uri)
            } else {
                CreateMapFromImageCommand(context, repo, name).execute(uri)
            }
        } finally {
            onMain {
                loadingIndicator.hide()
            }
        }
    }
}