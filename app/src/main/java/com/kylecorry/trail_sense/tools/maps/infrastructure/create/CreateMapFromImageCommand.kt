package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo

class CreateMapFromImageCommand(private val context: Context, private val repo: IMapRepo) {

    private val files = FileSubsystem.getInstance(context)

    suspend fun execute(uri: Uri): Map? = onIO {
        val defaultName = context.getString(android.R.string.untitled)
        val file = files.copyToLocal(uri, "maps") ?: return@onIO null
        var rotation = 0
        tryOrLog {
            val exif = ExifInterface(uri.toFile())
            rotation = exif.rotationDegrees
        }

        val map = Map(
            0,
            defaultName,
            files.getLocalPath(file),
            emptyList(),
            warped = false,
            rotated = false,
            rotation = rotation
        )

        val id = repo.addMap(map)
        map.copy(id = id)
    }

}