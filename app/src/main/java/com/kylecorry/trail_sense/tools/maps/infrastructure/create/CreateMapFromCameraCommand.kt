package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.graphics.Bitmap
import android.util.Size
import androidx.core.net.toUri
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo
import java.io.File
import java.util.*

class CreateMapFromCameraCommand(
    private val fragment: AndromedaFragment,
    private val repo: IMapRepo,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): Map? = onIO {
        var bitmap: Bitmap? = null
        var tempFile: File? = null
        try {
            bitmap = CustomUiUtils.takePhoto(fragment, Size(2048, 2048)) ?: return@onIO null
            onMain {
                loadingIndicator.show()
            }
            val filename = "tmp/" + UUID.randomUUID().toString() + ".jpg"
            tempFile = LocalFiles.getFile(fragment.requireContext(), filename, true)
            tempFile.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            bitmap.recycle()
            val map = CreateMapFromUriCommand(
                fragment.requireContext(),
                repo,
                tempFile.toUri(),
                loadingIndicator
            ).execute()
            tempFile.delete()
            map
        } finally {
            bitmap?.recycle()
            tempFile?.delete()
        }
    }
}