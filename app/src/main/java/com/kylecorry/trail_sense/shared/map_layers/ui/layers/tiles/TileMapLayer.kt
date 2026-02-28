package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.setBlendMode
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withSave
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.BackgroundTask
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.tiles.ImageTile
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileQueue
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

abstract class TileMapLayer<T : TileSource>(
    protected val source: T,
    override val layerId: String,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    private var minZoomLevel: Int? = null,
    private val shouldMultiply: Boolean = false,
    override val isTimeDependent: Boolean = false,
    private val refreshInterval: Duration? = null,
    private val refreshBroadcasts: List<String> = emptyList()
) : IAsyncLayer {
    private var _timeOverride: Instant? = null
    private var _renderTime: Instant = Instant.now()

    override fun setTime(time: Instant?) {
        _timeOverride = time
        refresh()
    }

    private var loader: TileLoader? = null
    private val queue = TileQueue()
    private val layerPaint = Paint()
    private val tilePaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = true
    }
    var multiplyAlpha: Int = 255
        set(value) {
            field = value
            layerPaint.alpha = value
        }
    private var updateListener: (() -> Unit)? = null
    private var zoomOffset: Int = 0
    private val renderMatrix = Matrix()
    private val srcPoints = FloatArray(8)
    private val dstPoints = FloatArray(8)
    private val srcRect = Rect()
    private val destRect = Rect()
    private val clipPath = Path()
    protected var layerPreferences: Bundle = bundleOf()
    private var featureId: String? = null

    private val loadTimer = CoroutineTimer {
        queue.load(16)
    }
    private val refreshTimer = refreshInterval?.let { CoroutineTimer { refresh() } }

    private val sourceCleanupTask = BackgroundTask {
        source.cleanup()
    }

    private fun onRefreshBroadcastReceived(data: Bundle): Boolean {
        refresh()
        return true
    }

    fun setZoomOffset(offset: Int) {
        zoomOffset = offset
    }

    fun setMinZoomLevel(level: Int) {
        minZoomLevel = level
    }

    init {
        if (shouldMultiply && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            layerPaint.setBlendMode(BlendModeCompat.MULTIPLY)
        } else {
            layerPaint.setBlendMode(BlendModeCompat.SRC_OVER)
        }
        taskRunner.addTask { _: Context, _: Rectangle, _: CoordinateBounds, projection: IMapViewProjection ->
            queue.setMapProjection(projection)
        }
    }

    open fun getBaseCacheKey(): String? {
        // Don't cache by default
        return null
    }

    fun getCacheKey(): String? {
        val baseKey = getBaseCacheKey() ?: return null
        return if (featureId != null) {
            "$baseKey-$featureId"
        } else {
            baseKey
        }
    }

    override fun setFeatureFilter(id: String?) {
        featureId = id
        refresh()
    }

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        // Avoid drawing while in safe mode
        if (SafeMode.isEnabled()) {
            return
        }

        // Load tiles if needed
        taskRunner.scheduleUpdate(
            context,
            drawer.getBounds(45f), // TODO: Cache this
            map.mapBounds,
            map.mapProjection
        )

        // Render loaded tiles
        var shouldSaveLayer = false
        try {
            if (shouldMultiply) {
                drawer.canvas.saveLayer(null, layerPaint)
                shouldSaveLayer = true
            }
            renderTiles(context, drawer.canvas, map)
        } finally {
            if (shouldSaveLayer) {
                drawer.pop()
            }
        }
    }

    override fun drawOverlay(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing, invalidation is handled separately
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun renderTiles(context: Context, canvas: Canvas, map: IMapView) {
        val bounds = map.mapBounds
        val projection = map.mapProjection
        val desiredTiles = getTiles(
            bounds,
            projection
        )

        queue.setDesiredTiles(desiredTiles)
        if (desiredTiles.size <= MAX_TILES &&
            (desiredTiles.firstOrNull()?.z ?: 0) >= (minZoomLevel ?: 0)
        ) {
            loader?.loadTiles(desiredTiles, _renderTime, layerPreferences, featureId, context)
        } else if (desiredTiles.size > MAX_TILES) {
            Log.d("TileLoader", "Too many tiles to load: ${desiredTiles.size}")
        }

        getTilesToRender(desiredTiles).forEach { renderTile ->
            renderTile.imageTile.withImage { bitmap ->
                bitmap ?: return@withImage
                tryOrLog {
                    val clipTile = renderTile.clipTo
                    if (clipTile != null) {
                        renderTileClipped(
                            renderTile.imageTile,
                            canvas,
                            projection,
                            bitmap,
                            clipTile
                        )
                    } else {
                        renderTile(renderTile.imageTile, canvas, projection, bitmap)
                    }
                }
            }
        }
    }

    private fun getTilesToRender(desiredTiles: List<Tile>): List<RenderTile> {
        val toRender = mutableSetOf<RenderTile>()
        desiredTiles.forEach { desired ->
            getTilesToRender(desired).forEach { imageTile ->
                if (imageTile.tile.z < desired.z) {
                    toRender.add(RenderTile(imageTile, desired))
                } else {
                    toRender.add(RenderTile(imageTile))
                }
            }
        }
        return toRender.sortedBy { it.imageTile.tile.z }
    }

    private fun getTilesToRender(desiredTile: Tile): List<ImageTile> {
        val tiles = mutableListOf<ImageTile>()
        val self = loader?.tileCache?.get(desiredTile)
        if (isTileAvailable(self)) {
            tiles.add(self!!)
            if (!self.isFadingIn()) {
                return tiles
            }
        }

        // Try to replace with the direct children tiles
        val children = findChildren(desiredTile)
        tiles.addAll(children)
        if (children.size >= 4) {
            return tiles
        }

        // Try to find the parent tile(s)
        val parent = findParent(desiredTile)
        if (parent != null) {
            tiles.add(parent)
        }

        return tiles
    }

    private fun findChildren(tile: Tile): List<ImageTile> {
        return tile.getChildren()
            .mapNotNull { loader?.tileCache?.peek(it) }
            .filter { isTileAvailable(it) }
    }

    private fun findParent(tile: Tile): ImageTile? {
        var parent = tile.getParent()
        while (parent != null) {
            val parentImageTile = loader?.tileCache?.peek(parent)
            if (isTileAvailable(parentImageTile)) {
                return parentImageTile
            }
            parent = parent.getParent()
        }
        return null
    }

    private fun isTileAvailable(tile: ImageTile?): Boolean {
        return tile?.state == TileState.Loaded ||
                tile?.state == TileState.Empty ||
                tile?.state == TileState.Stale ||
                (tile?.state == TileState.Loading && tile.hasImage())
    }

    private fun isTooSmall(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate
    ): Boolean {
        val minSize = 10f
        val width = max(topRight.x, bottomRight.x) - min(topLeft.x, bottomLeft.x)
        val height = max(bottomLeft.y, bottomRight.y) - min(topLeft.y, topRight.y)
        return width < minSize || height < minSize
    }

    private fun isFarOffScreen(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate,
        canvasWidth: Int,
        canvasHeight: Int
    ): Boolean {
        val buffer = canvasHeight.coerceAtLeast(canvasWidth) * 2
        val minX = minOf(topLeft.x, bottomLeft.x)
        val maxX = maxOf(topRight.x, bottomRight.x)
        val minY = minOf(topLeft.y, topRight.y)
        val maxY = maxOf(bottomLeft.y, bottomRight.y)

        return maxX < -buffer ||
                minX > canvasWidth + buffer ||
                maxY < -buffer ||
                minY > canvasHeight + buffer
    }

    private fun renderTile(
        imageTile: ImageTile,
        canvas: Canvas,
        projection: IMapViewProjection,
        bitmap: Bitmap
    ) {
        val tile = imageTile.tile
        val bounds = tile.getBounds()
        val topLeftPixel = projection.toPixels(bounds.northWest)
        val topRightPixel = projection.toPixels(bounds.northEast)
        val bottomRightPixel = projection.toPixels(bounds.southEast)
        val bottomLeftPixel = projection.toPixels(bounds.southWest)

        if (isTooSmall(topLeftPixel, topRightPixel, bottomLeftPixel, bottomRightPixel)) {
            return
        }

        if (isFarOffScreen(
                topLeftPixel,
                topRightPixel,
                bottomLeftPixel,
                bottomRightPixel,
                canvas.width,
                canvas.height
            )
        ) {
            return
        }

        val borderPixels = TILE_BORDER_PIXELS
        val actualWidth = bitmap.width - 2 * borderPixels
        val actualHeight = bitmap.height - 2 * borderPixels

        // Bitmap pixels, exclude the border pixels - this is the bitmap with the borders cropped off (since we only render a region of the bitmap)
        // Top left
        srcPoints[0] = 0f
        srcPoints[1] = 0f
        // Top right
        srcPoints[2] = actualWidth.toFloat()
        srcPoints[3] = 0f
        // Bottom left
        srcPoints[4] = 0f
        srcPoints[5] = actualHeight.toFloat()
        // Bottom right
        srcPoints[6] = actualWidth.toFloat()
        srcPoints[7] = actualHeight.toFloat()

        // Canvas pixels
        // Top left
        dstPoints[0] = topLeftPixel.x
        dstPoints[1] = topLeftPixel.y
        // Top right
        dstPoints[2] = topRightPixel.x
        dstPoints[3] = topRightPixel.y
        // Bottom left
        dstPoints[4] = bottomLeftPixel.x
        dstPoints[5] = bottomLeftPixel.y
        // Bottom right
        dstPoints[6] = bottomRightPixel.x
        dstPoints[7] = bottomRightPixel.y

        renderMatrix.reset()
        renderMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        canvas.withMatrix(renderMatrix) {
            // The region of the bitmap that is valid (no borders)
            srcRect.set(
                borderPixels,
                borderPixels,
                bitmap.width - borderPixels,
                bitmap.height - borderPixels
            )
            // The actual bitmap dimensions (after cropping)
            destRect.set(
                0,
                0,
                actualWidth,
                actualHeight
            )

            // Calculate alpha for fade-in effect
            val originalAlpha = tilePaint.alpha
            tilePaint.alpha = imageTile.getAlpha()
            // There are still tiles being faded in, so keep re-rendering the map
            if (tilePaint.alpha != 255) {
                notifyListeners()
            }

            drawBitmap(bitmap, srcRect, destRect, tilePaint)

            tilePaint.alpha = originalAlpha
        }
    }

    private fun renderTileClipped(
        imageTile: ImageTile,
        canvas: Canvas,
        projection: IMapViewProjection,
        bitmap: Bitmap,
        clipTile: Tile
    ) {
        val clipBounds = clipTile.getBounds()
        val clipTopLeft = projection.toPixels(clipBounds.northWest)
        val clipTopRight = projection.toPixels(clipBounds.northEast)
        val clipBottomRight = projection.toPixels(clipBounds.southEast)
        val clipBottomLeft = projection.toPixels(clipBounds.southWest)

        canvas.withSave {
            clipPath.rewind()
            clipPath.moveTo(clipTopLeft.x, clipTopLeft.y)
            clipPath.lineTo(clipTopRight.x, clipTopRight.y)
            clipPath.lineTo(clipBottomRight.x, clipBottomRight.y)
            clipPath.lineTo(clipBottomLeft.x, clipBottomLeft.y)
            clipPath.close()
            clipPath(clipPath)
            renderTile(imageTile, this, projection, bitmap)
        }
    }

    private fun getTiles(bounds: CoordinateBounds, projection: IMapViewProjection): List<Tile> {
        val zoom = projection.zoom.roundToInt()
        var adjustedOffset = zoomOffset + 1
        var tiles: List<Tile>
        do {
            adjustedOffset--
            tiles = TileMath.getTiles(bounds, (zoom + adjustedOffset).coerceAtMost(20))
        } while (tiles.size > MAX_TILES && (zoom + adjustedOffset) > 1)
        return tiles
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    override fun start() {
        refreshTime()
        loader = TileLoader(
            source,
            queue,
            TILE_BORDER_PIXELS,
            tag = layerId,
            key = getCacheKey()
        ) {
            notifyListeners()
        }
        loadTimer.interval(100)
        refreshBroadcasts.forEach {
            Tools.subscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshInterval?.let { refreshTimer?.interval(it, it) }
    }

    override fun stop() {
        loadTimer.stop()
        refreshBroadcasts.forEach {
            Tools.unsubscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshTimer?.stop()
        taskRunner.stop()
        loader?.clearCache()
        loader = null
        queue.clear()
        // TODO: This isn't the ideal place to do cleanup since garbage can build up. Likely need some sort of LRU cache for all intermediates.
        sourceCleanupTask.start()
    }

    fun refresh() {
        refreshTime()
        loadTimer.stop()
        queue.clear()
        loader?.tileCache?.snapshot()?.forEach {
            it.value.invalidate()
        }
        loadTimer.interval(100)
        invalidate()
        notifyListeners()
    }

    fun improveResolution(
        bounds: CoordinateBounds,
        zoom: Int,
        minimumTileCount: Int
    ) {
        var tileCount: Int
        var zoomOffset = -1
        do {
            zoomOffset++
            tileCount = TileMath.getTiles(bounds, zoom + zoomOffset).size
        } while (tileCount < minimumTileCount && zoomOffset < 10)

        setZoomOffset(zoomOffset)
        notifyListeners()
    }

    private fun refreshTime() {
        _renderTime = _timeOverride ?: Instant.now()
    }

    override fun setPreferences(preferences: Bundle) {
        layerPreferences = Bundle(preferences)
        if (shouldMultiply) {
            multiplyAlpha = Interpolation.map(
                preferences.getInt(
                    DefaultMapLayerDefinitions.OPACITY,
                    DefaultMapLayerDefinitions.DEFAULT_OPACITY
                ) / 100f,
                0f,
                1f,
                0f,
                255f,
                shouldClamp = true
            ).toInt()
        } else {
            percentOpacity = preferences.getInt(
                DefaultMapLayerDefinitions.OPACITY,
                DefaultMapLayerDefinitions.DEFAULT_OPACITY
            ) / 100f
        }
    }

    override var percentOpacity: Float = 1f

    private data class RenderTile(
        val imageTile: ImageTile,
        val clipTo: Tile? = null
    )

    companion object {
        const val MAX_TILES = 150
        private const val TILE_BORDER_PIXELS = 2
    }
}
