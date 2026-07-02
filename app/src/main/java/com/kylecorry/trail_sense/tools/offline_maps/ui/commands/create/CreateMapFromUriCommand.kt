package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

class CreateMapFromUriCommand(
    private val context: Context,
    private val uri: Uri,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun execute(): OfflineMapCatalogItem? = onIO {
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
            val type = files.getMimeType(uri) ?: getMimeTypeFromExtension(uri)
            if (type == MIME_TYPE_PDF) {
                CreateMapFromPDFCommand(context, name).execute(uri)
            } else if (type?.startsWith(MIME_TYPE_IMAGE_PREFIX) == true) {
                CreateMapFromImageCommand(context, name).execute(uri)
            } else {
                CreateTrailMapFromFileCommand(name).execute(uri)
            }
        } finally {
            onMain {
                loadingIndicator.hide()
            }
        }
    }

    private fun getMimeTypeFromExtension(uri: Uri): String? {
        val filename = files.getFileName(uri, withExtension = true, fallbackToPathName = true)
            ?: uri.lastPathSegment
            ?: return null

        return when (filename.substringAfterLast('.', "").lowercase()) {
            "pdf" -> MIME_TYPE_PDF
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> null
        }
    }

    private companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val MIME_TYPE_IMAGE_PREFIX = "image/"
    }
}
