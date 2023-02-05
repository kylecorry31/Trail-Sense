package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo

class CreateMapFromImageCommand(context: Context, private val repo: IMapRepo, private val name: String) {

    private val files = FileSubsystem.getInstance(context)

    suspend fun execute(uri: Uri): PhotoMap? = onIO {
        val file = files.copyToLocal(uri, "maps") ?: return@onIO null
        var rotation = 0
        tryOrLog {
            val exif = ExifInterface(uri.toFile())
            rotation = exif.rotationDegrees
        }

        val path = files.getLocalPath(file)
        val imageSize = files.imageSize(path)
        val fileSize = files.size(path)

        val map = PhotoMap(
            0,
            name,
            path,
            MapCalibration.uncalibrated().copy(rotation = rotation),
            MapMetadata(
                Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
                fileSize
            )
        )

        val id = repo.addMap(map)
        map.copy(id = id)
    }

}