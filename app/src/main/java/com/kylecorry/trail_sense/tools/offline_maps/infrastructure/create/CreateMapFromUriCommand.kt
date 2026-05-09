package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo

class CreateMapFromUriCommand(
    private val context: Context,
    private val repo: MapRepo,
    private val uri: Uri,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun execute(): IMap? = onIO {
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
                CreateMapFromPDFCommand(context, repo, name).execute(uri)
            } else if (type?.startsWith(MIME_TYPE_IMAGE_PREFIX) == true) {
                CreateMapFromImageCommand(context, repo, name).execute(uri)
            } else {
                CreateVectorMapFromFileCommand(repo, name).execute(uri)
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
