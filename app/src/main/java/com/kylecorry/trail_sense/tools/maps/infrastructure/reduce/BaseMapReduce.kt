package com.kylecorry.trail_sense.tools.maps.infrastructure.reduce

import android.content.Context
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import java.util.*

abstract class BaseMapReduce(
    context: Context,
    private val quality: Int,
    private val maxSize: Size?
) : IMapReduce {

    private val mapRepo = MapRepo.getInstance(context)
    private val files = FileSubsystem.getInstance(context)

    override suspend fun reduce(map: PhotoMap) = onIO {
        val bmp = files.bitmap(map.filename, maxSize?.toAndroidSize()) ?: return@onIO
        files.save(map.filename, bmp, quality, true)
        if (!map.filename.endsWith(".webp")) {
            val newFileName = "maps/" + UUID.randomUUID().toString() + ".webp"
            if (files.rename(map.filename, newFileName)) {
                mapRepo.addMap(map.copy(filename = newFileName))
            }
        }
    }

}