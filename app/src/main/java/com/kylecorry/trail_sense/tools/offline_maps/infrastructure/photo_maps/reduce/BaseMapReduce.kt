package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.MapService
import java.util.UUID

abstract class BaseMapReduce(
    context: Context,
    private val quality: Int,
    private val maxSize: Size?
) : IMapReduce {

    private val service = getAppService<MapService>()
    private val files = FileSubsystem.getInstance(context)

    override suspend fun reduce(map: PhotoMap) = onIO {
        val originalFileName = map.imageFile.path
        val bmp = files.bitmap(originalFileName, maxSize?.toAndroidSize()) ?: return@onIO
        files.save(originalFileName, bmp, quality, true)

        // Remove the PDF
        map.pdfFile?.let { files.delete(it.path) }

        val newFileName = if (!originalFileName.endsWith(".webp")) {
            val newName = "maps/" + UUID.randomUUID().toString() + ".webp"
            if (files.rename(originalFileName, newName)) {
                newName
            } else {
                originalFileName
            }
        } else {
            originalFileName
        }
        val newFiles = listOf(
            OfflineMapFile(newFileName, files.size(newFileName), PhotoMap.FILE_ROLE_IMAGE)
        )
        val updatedMap = map.copy(files = newFiles)
        service.add(updatedMap)
    }

}
