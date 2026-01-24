package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.alpha
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.Convert
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import java.util.concurrent.ConcurrentLinkedQueue

class PhotoMapTileSourceSelector(
    private val context: Context,
    maps: List<PhotoMap>,
    private val decoderCache: PhotoMapDecoderCache,
    private val maxLayers: Int = 4,
    private val loadPdfs: Boolean = true,
    private val isPixelPerfect: Boolean = false,
    private val backgroundColor: Int = Color.WHITE,
    private val operations: List<BitmapOperation> = emptyList()
) : TileSource {

    private val sortedMaps = maps
        .filter { it.isCalibrated }
        .sortedBy { it.distancePerPixel() }

    private var recentMaps = ConcurrentLinkedQueue<PhotoMap>()

    override suspend fun loadTile(tile: Tile): Bitmap? {
        val loaders = getRegionLoaders(tile.getBounds())

        val maps = loaders.map { it.map }.distinct()
        recentMaps.addAll(maps)

        return loadTile(tile, loaders)
    }

    override suspend fun cleanup() {
        super.cleanup()
        val lastUsed = mutableListOf<PhotoMap>()
        while (recentMaps.isNotEmpty()) {
            recentMaps.poll()?.let { lastUsed.add(it) }
        }

        decoderCache.recycleInactive(lastUsed)
    }

    private suspend fun loadTile(tile: Tile, loaders: List<PhotoMapRegionLoader>): Bitmap? {
        if (loaders.isEmpty()) return null

        val config = if (backgroundColor.alpha != 255) {
            Bitmap.Config.ARGB_8888
        } else {
            Bitmap.Config.RGB_565
        }
        val image = createBitmap(tile.size.width, tile.size.height, config)
        image.eraseColor(backgroundColor)
        val canvas = Canvas(image)

        loaders.reversed().forEachIndexed { index, loader ->
            loader.load(tile)?.applyOperationsOrNull(
                Conditional(
                    index > 0,
                    ReplaceColor(
                        Color.WHITE,
                        Color.argb(127, 127, 127, 127),
                        80f,
                        true,
                        inPlace = true
                    )
                )
            )?.use {
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }

        // Remove transparency
        return image.applyOperationsOrNull(
            // Undo color replacement
            Conditional(
                backgroundColor.alpha != 255 && loaders.size > 1,
                ReplaceColor(
                    Color.argb(127, 127, 127, 127),
                    Color.WHITE,
                    80f,
                    true,
                    inPlace = true
                )
            ),
            Convert(config)
        )
    }

    // TODO: Factor in rotation by using projection to see if the bounds intersect/are contained
    private fun getRegionLoaders(bounds: CoordinateBounds): List<PhotoMapRegionLoader> {
        val minArea = bounds.width().meters().value.toDouble() * bounds.height()
            .meters().value.toDouble() * 0.25

        val possibleMaps = sortedMaps.filter {
            val boundary = it.boundary() ?: return@filter false
            if (boundary == CoordinateBounds.world) {
                return@filter true
            }
            val area = boundary.width().meters().value.toDouble() *
                    boundary.height().meters().value.toDouble()
            area >= minArea
        }

        val firstContained = possibleMaps.firstOrNull {
            contains(
                it.boundary() ?: return@firstOrNull false,
                bounds,
                fullyContained = true
            )
        }

        val containedMaps = possibleMaps.filter {
            contains(
                it.boundary() ?: return@filter false,
                bounds
            )
        }.take(maxLayers).toMutableList()


        val maps = if (firstContained != null && !containedMaps.contains(firstContained)) {
            if (containedMaps.size == maxLayers) {
                containedMaps.removeLastOrNull()
            }
            containedMaps.add(firstContained)
            containedMaps
        } else if (firstContained != null && SolMath.isZero(
                firstContained.baseRotation() - firstContained.calibration.rotation,
                0.5f
            )
        ) {
            // The contained map isn't really rotated so only include a map after it
            val index = containedMaps.indexOf(firstContained)
            containedMaps.subList(
                0,
                minOf(index + 2, containedMaps.size)
            )
        } else {
            containedMaps
        }

        return maps.map {
            PhotoMapRegionLoader(
                context,
                it,
                decoderCache,
                loadPdfs,
                isPixelPerfect,
                operations
            )
        }
    }

    // TODO: Extract to sol
    private fun contains(
        bounds: CoordinateBounds,
        subBounds: CoordinateBounds,
        fullyContained: Boolean = false
    ): Boolean {

        return if (fullyContained) {
            val corners = listOf(
                bounds.contains(subBounds.northWest),
                bounds.contains(subBounds.northEast),
                bounds.contains(subBounds.southWest),
                bounds.contains(subBounds.southEast),
                bounds.contains(subBounds.center)
            )
            corners.all { it }
        } else {
            bounds.intersects(subBounds)
        }
    }

}