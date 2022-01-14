package com.kylecorry.trail_sense.tools.maps.infrastructure.reduce

import android.content.Context
import android.graphics.BitmapFactory
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.ImageSaver
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import java.io.File
import java.io.FileOutputStream
import java.util.*

abstract class BaseMapReduce(
    private val context: Context,
    private val quality: Int,
    private val maxSize: Size?
) : IMapReduce {

    private val saver = ImageSaver()
    private val mapRepo = MapRepo.getInstance(context)

    override suspend fun reduce(map: Map) {
        val file = LocalFiles.getFile(context, map.filename, false)
        val bmp = if (maxSize != null) {
            BitmapUtils.decodeBitmapScaled(file.path, maxSize.width.toInt(), maxSize.height.toInt())
        } else {
            BitmapFactory.decodeFile(file.path)
        }
        try {
            FileOutputStream(file).use {
                saver.save(bmp, it, quality)
            }
            if (!map.filename.endsWith(".webp")){
                val newFileName = "maps/" + UUID.randomUUID().toString() + ".webp"
                val renamed = File(context.filesDir, newFileName)
                val success = file.renameTo(renamed)
                if (success){
                    mapRepo.addMap(map.copy(filename = newFileName))
                }
            }

        } finally {
            bmp.recycle()
        }
    }

}