package com.kylecorry.trail_sense.shared.canvas.tiles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.math.SolMath.roundNearest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap


class BitmapTileLoader(private val path: String, private val scaleStep: Float = 0.01f) :
    TileLoader {

    override val tiles: List<Pair<ImageTile, Bitmap>>
        get() = listOfNotNull(baseImage) + tileMap.toList()

    private var baseImage: Pair<ImageTile, Bitmap>? = null
    private val tileMap = ConcurrentHashMap<ImageTile, Bitmap>()
    private val tileUpdateQueue = CoroutineQueueRunner()
    private var imageSize = Size(0, 0)
    private var onTilesChangedListener: (() -> Unit)? = null

    // TODO: There are gaps between tiles
    override suspend fun updateTiles(zoom: Float, clipBounds: RectF) {
        onDefault {
            tileUpdateQueue.enqueue {
                onIO {
                    if (imageSize.width == 0 || imageSize.height == 0) {
                        imageSize = BitmapUtils.getBitmapSize(path)!!
                    }

                    if (baseImage == null) {
                        baseImage = ImageTile(0, 0, imageSize.width, imageSize.height) to
                                renderTile(
                                    path,
                                    ImageTile(0, 0, imageSize.width, imageSize.height),
                                    256
                                )!!
                    }

                    val tileSize = TileCreator.getTileSize(
                        imageSize.width,
                        imageSize.height,
                        zoom.roundNearest(scaleStep),
                    ).width
                    val allTiles =
                        TileCreator.createTiles(
                            imageSize.width,
                            imageSize.height,
                            tileSize
                        )

                    // TODO: Why is the size off by 1?
                    val tiles = TileCreator.clip(
                        allTiles,
                        clipBounds
                    ).map {
                        it.copy(width = it.width + 1, height = it.height + 1)
                    }

                    val adjustedBaseImageTile = baseImage!!.first.copy(
                        width = baseImage!!.first.width + 1,
                        height = baseImage!!.first.height + 1
                    )
                    val tilesToLoad =
                        tiles.filter { !tileMap.containsKey(it) && it != adjustedBaseImageTile }
                    val tilesToRemove = tileMap.keys.filter { !tiles.contains(it) }

                    val loadedTiles = ConcurrentHashMap<ImageTile, Bitmap>()

                    // TODO: Replace tile tree when loaded
                    val jobs = tilesToLoad.map { tile ->
                        launch {
                            loadedTiles[tile] = renderTile(path, tile, 256)!!
                        }
                    }

                    // Wait for all jobs to finish
                    jobs.joinAll()

                    tileMap.putAll(loadedTiles)
                    tilesToRemove.forEach {
                        tileMap.remove(it)
                    }
//                println("Tile size: $tileSize")
//                println("Tile count: ${tiles.size} / ${allTiles.size}")
//                println("Cache size: ${tileMap.size}")
//                println("Added: ${tilesToLoad.size}, Removed: ${tilesToRemove.size}")
//                println("Max bitmap size: ${tileMap.maxOf { it.value.width }}")
//                println("Min bitmap size: ${tileMap.minOf { it.value.width }}")
//                println()
                }

                onMain {
                    onTilesChangedListener?.invoke()
                }
            }
        }
    }

    private fun renderTile(path: String, tile: ImageTile, destinationSize: Int): Bitmap? {
        return FileInputStream(File(path)).use {
            val options = BitmapFactory.Options().also {
                it.inSampleSize = calculateInSampleSize(
                    tile.width,
                    tile.height,
                    destinationSize,
                    destinationSize
                )
                it.inScaled = true
            }
            BitmapUtils.decodeRegion(
                it,
                Rect(tile.x, tile.y, tile.x + tile.width, tile.y + tile.height),
                options
            )
        }
    }

    private fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        var inSampleSize = 1

        if (sourceHeight > reqHeight || sourceWidth > reqWidth) {

            val halfHeight: Int = sourceHeight / 2
            val halfWidth: Int = sourceWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun release() {
        tileUpdateQueue.cancel()
        tileMap.values.forEach {
            it.recycle()
        }
        baseImage?.second?.recycle()
        baseImage = null
        tileMap.clear()
    }

    override fun setOnTilesChangedListener(listener: () -> Unit) {
        onTilesChangedListener = listener
    }
}