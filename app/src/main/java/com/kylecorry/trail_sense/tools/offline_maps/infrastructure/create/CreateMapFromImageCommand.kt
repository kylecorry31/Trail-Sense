package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibration
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapMetadata
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import java.time.Instant

class CreateMapFromImageCommand(
    context: Context,
    private val name: String
) {
    private val files = FileSubsystem.getInstance(context)
    private val service = getAppService<MapService>()

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
            MapCalibration.uncalibrated().copy(rotation = rotation.toFloat()),
            MapMetadata(
                Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
                null,
                fileSize
            ),
            createdOn = Instant.now()
        )

        val id = service.add(map)
        map.copy(id = id)
    }

}
