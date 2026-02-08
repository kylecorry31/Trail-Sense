package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.ImageSaver
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

class PersistentTileCache(context: Context) {

    private val appContext = context.applicationContext
    private val repo = CachedTileRepo.getInstance(appContext)
    private val files = CacheFileSystem(appContext)
    private val imageSaver = ImageSaver()

    suspend fun getOrPut(key: String, tile: Tile, producer: suspend () -> Bitmap): Bitmap = onIO {
        val cached = repo.get(key, tile)

        if (cached != null) {
            // Cache hit - load bitmap and update last used time
            val bitmap = loadBitmap(cached)
            if (bitmap != null) {
                repo.updateLastUsedOn(cached.id)
                return@onIO bitmap
            }
            // File was missing, delete the cache entry
            repo.delete(cached)
        }

        // Cache miss - produce bitmap and save
        val bitmap = producer()
        saveBitmap(key, tile, bitmap)

        // Check if cache exceeds limit and evict if needed
        val totalSize = repo.getTotalSizeBytes()
        if (totalSize > MAX_CACHE_SIZE_BYTES) {
            repo.deleteLeastRecentlyUsed(EVICTION_COUNT)
            CachedTileCleanupWorker.start(appContext)
        }

        bitmap
    }

    suspend fun invalidate(key: String) = onIO {
        val filenames = repo.getFilenamesByKey(key)
        repo.deleteByKey(key)
        for (filename in filenames) {
            files.delete("${CachedTileRepo.TILES_FOLDER}/$filename")
        }
    }

    private fun loadBitmap(cached: CachedTile): Bitmap? {
        val file = files.getFile("${CachedTileRepo.TILES_FOLDER}/${cached.filename}", false)
        if (!file.exists()) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = if (cached.hasAlpha) {
                Bitmap.Config.ARGB_8888
            } else {
                Bitmap.Config.RGB_565
            }
        }

        return BitmapFactory.decodeFile(file.path, options)
    }

    private suspend fun saveBitmap(key: String, tile: Tile, bitmap: Bitmap) {
        val filename = "${UUID.randomUUID()}.webp"
        val file = files.getFile("${CachedTileRepo.TILES_FOLDER}/$filename", create = true)

        FileOutputStream(file).use { out ->
            imageSaver.save(bitmap, out, LOSSLESS_QUALITY)
        }

        val hasAlpha = bitmap.config == Bitmap.Config.ARGB_8888
        val sizeBytes = file.length()
        val now = Instant.now()

        val cachedTile = CachedTile(
            id = 0,
            key = key,
            tile = tile,
            filename = filename,
            createdOn = now,
            lastUsedOn = now,
            sizeBytes = sizeBytes,
            hasAlpha = hasAlpha
        )

        repo.add(cachedTile)
    }

    companion object {
        private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
        private const val EVICTION_COUNT = 150
        private const val LOSSLESS_QUALITY = 100
    }
}
