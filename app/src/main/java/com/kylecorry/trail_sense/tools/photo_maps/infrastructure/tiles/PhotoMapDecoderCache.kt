package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.shared.canvas.tiles.ImageRegionDecoder
import com.kylecorry.trail_sense.shared.canvas.tiles.PdfImageRegionDecoder
import com.kylecorry.trail_sense.shared.canvas.tiles.RegionDecoder
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class PhotoMapDecoderCache {
    private val loaders = ConcurrentHashMap<PhotoMap, RegionDecoder>()
    private val mutexes = ConcurrentHashMap<PhotoMap, Mutex>()
    private val files = AppServiceRegistry.get<FileSubsystem>()

    private fun getLock(map: PhotoMap): Mutex {
        return mutexes.getOrPut(map) { Mutex() }
    }

    private suspend fun getPdfLoader(
        context: Context,
        map: PhotoMap
    ): RegionDecoder {
        loaders[map]?.let { return it }

        return getLock(map).withLock {
            loaders[map]?.let { return it }
            val decoder = PdfImageRegionDecoder(Bitmap.Config.ARGB_8888)
            decoder.init(context, files.uri(map.pdfFileName))
            loaders[map] = decoder
            decoder
        }
    }

    private suspend fun getImageLoader(
        context: Context,
        map: PhotoMap
    ): RegionDecoder {
        loaders[map]?.let { return it }

        return getLock(map).withLock {
            loaders[map]?.let { return it }
            val decoder = ImageRegionDecoder(context, Bitmap.Config.ARGB_8888)

            if (map.isAsset) {
                decoder.initFromAsset(map.filename.removePrefix(files.SCHEME_ASSETS))
            } else {
                decoder.init(files.uri(map.filename))
            }

            loaders[map] = decoder
            decoder
        }
    }

    suspend fun decodeRegion(
        context: Context,
        map: PhotoMap,
        region: Rect,
        sampleSize: Int,
        isPdf: Boolean
    ): Bitmap? {
        return tryOrDefault(null) {
            val loader = if (isPdf) {
                getPdfLoader(context, map)
            } else {
                getImageLoader(context, map)
            }
            loader.decodeRegionSuspend(region, sampleSize)
        }
    }

    suspend fun recycleInactive(activeMaps: List<PhotoMap>) {
        val activeSet = activeMaps.toSet()

        loaders.keys
            .filter { it !in activeSet }
            .forEach { map ->
                getLock(map).withLock {
                    loaders.remove(map)?.recycleSuspend()
                    mutexes.remove(map)
                }
            }
    }
}
