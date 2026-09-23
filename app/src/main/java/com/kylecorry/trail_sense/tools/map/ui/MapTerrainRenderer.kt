package com.kylecorry.trail_sense.tools.map.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.min
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt

internal class MapTerrainRenderer(private val map: MapView) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var cameraKey: List<Any>? = null
    private var meshTilt = Float.NaN
    private var textureKey: List<Any?>? = null
    private var bitmap: Bitmap? = null
    private var textureDrawer: CanvasDrawer? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var mesh: TerrainMesh? = null
    private var terrain: DEM.ElevationBitmap? = null
    private var minimumElevation = 0f
    private var maximumElevation = 0f
    val groundHeight: Float
        get() = mesh?.groundHeight ?: map.height * 3f
    val groundWidth: Float
        get() = mesh?.groundWidth ?: map.width.toFloat()
    private var terrainResolution = 0f

    fun draw(canvas: Canvas) {
        if (map.width == 0 || map.height == 0) return
        val projection = map.mapProjection
        val azimuth = map.mapAzimuth
        val nextCameraKey = listOf(projection, azimuth, map.width, map.height)
        if (cameraKey != nextCameraKey) {
            cameraKey = nextCameraKey
            job?.cancel()
            meshTilt = map.tiltDegrees
            val covered = updateMesh(projection, azimuth)
            if (!covered || projection.resolutionPixels / terrainResolution !in 0.5f..2f) {
                loadTerrain(projection, azimuth, nextCameraKey)
            }
        } else if (meshTilt != map.tiltDegrees) {
            meshTilt = map.tiltDegrees
            updateMesh(projection, azimuth)
        }

        // Scale both texture dimensions while keeping projection and touch coordinates unchanged.
        val textureWidth = (groundWidth * TEXTURE_RESOLUTION_SCALE).roundToInt().coerceAtLeast(1)
        val textureHeight = (groundHeight * TEXTURE_RESOLUTION_SCALE).roundToInt().coerceAtLeast(1)
        if (bitmap?.width != textureWidth || bitmap?.height != textureHeight) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888)
            textureDrawer = CanvasDrawer(map.context, Canvas(bitmap!!))
            textureKey = null
        }
        val texture = bitmap ?: return
        val drawer = textureDrawer ?: return
        val nextTextureKey = listOf(nextCameraKey, map.layerManager.revision, map.userLocation,
            map.userLocationAccuracy, map.userAzimuth, map.backgroundColorOverride, groundHeight, groundWidth)
        if (textureKey != nextTextureKey) {
            // Store before drawing so asynchronous layer updates cannot be lost.
            textureKey = nextTextureKey
            texture.eraseColor(map.backgroundColorOverride ?: Color.rgb(127, 127, 127))
            drawer.canvas.save()
            drawer.canvas.scale(textureWidth / groundWidth, textureHeight / groundHeight)
            drawer.canvas.translate((groundWidth - map.width) / 2f, (groundHeight - map.height) / 2f)
            drawer.canvas.rotate(-azimuth, map.width / 2f, map.height / 2f)
            map.layerManager.draw(map.context, drawer, map)
            drawer.canvas.restore()
        }
        mesh?.let {
            canvas.drawBitmapMesh(texture, TerrainMesh.COLUMNS, TerrainMesh.ROWS,
                it.vertices, 0, null, 0, paint)
        }
    }

    private fun updateMesh(projection: IMapViewProjection, azimuth: Float): Boolean {
        val centerElevation = terrain?.sample(projection.center) ?: 0f
        val elevationRange = max(abs(minimumElevation - centerElevation), abs(maximumElevation - centerElevation))
        val requiredHeight = max(map.height * 3f,
            TerrainMesh.groundHeight(map.height.toFloat(), elevationRange,
                projection.resolutionPixels, TerrainMesh.MAX_TILT_DEGREES))
        val requiredWidth = TerrainMesh.groundWidth(map.width.toFloat(), map.height.toFloat(),
            requiredHeight, TerrainMesh.MAX_TILT_DEGREES)
        val previousHeight = groundHeight
        val previousWidth = groundWidth
        val flat = TerrainMesh(map.width.toFloat(), map.height.toFloat(),
            FloatArray(TerrainMesh.VERTEX_COUNT), projection.resolutionPixels, requiredHeight,
            map.tiltDegrees, requiredWidth)
        var covered = true
        val elevations = FloatArray(TerrainMesh.VERTEX_COUNT)
        for (row in 0..TerrainMesh.ROWS) {
            for (column in 0..TerrainMesh.COLUMNS) {
                val point = flat.source(column, row)
                val x = point.x - flat.width / 2
                val y = point.y - flat.height / 2
                val location = projection.toCoordinate(Vector2(
                    x * cosDegrees(azimuth) - y * sinDegrees(azimuth) + flat.width / 2,
                    x * sinDegrees(azimuth) + y * cosDegrees(azimuth) + flat.height / 2
                ))
                val elevation = terrain?.sample(location)
                if (elevation == null) covered = false
                elevations[row * (TerrainMesh.COLUMNS + 1) + column] = elevation ?: 0f
            }
        }
        // Keep cached terrain during ordinary pans, rotations, and zooms.
        mesh = if (covered) TerrainMesh(flat.width, flat.height, elevations,
            projection.resolutionPixels, requiredHeight, map.tiltDegrees, requiredWidth) else flat
        if (previousHeight != requiredHeight || previousWidth != requiredWidth) map.layerManager.invalidate()
        return covered
    }

    private fun loadTerrain(projection: IMapViewProjection, azimuth: Float, key: List<Any>) {
        val width = map.width.toFloat()
        val height = map.height.toFloat()
        val groundHeight = groundHeight
        val groundWidth = groundWidth
        job = scope.launch {
            delay(120)
            try {
                val loaded = withContext(Dispatchers.Default) {
                    val corners = listOf(-groundWidth to -groundHeight, groundWidth to -groundHeight,
                        -groundWidth to groundHeight, groundWidth to groundHeight).map { (x, y) ->
                        projection.toCoordinate(Vector2(
                            x * cosDegrees(azimuth) - y * sinDegrees(azimuth) + width / 2,
                            x * sinDegrees(azimuth) + y * cosDegrees(azimuth) + height / 2
                        ))
                    }
                    val bounds = CoordinateBounds.from(corners)
                    val resolution = max(bounds.widthDegrees() / 64, bounds.heightDegrees() / 128)
                        .coerceAtLeast(0.000001)
                    DEM.getElevations(bounds, resolution)
                        .let { grid ->
                            var minimum = Float.POSITIVE_INFINITY
                            var maximum = Float.NEGATIVE_INFINITY
                            for (y in 0 until grid.data.height) {
                                for (x in 0 until grid.data.width) {
                                    val value = grid.data.get(x, y, 0)
                                    if (value.isFinite()) {
                                        minimum = min(minimum, value)
                                        maximum = max(maximum, value)
                                    }
                                }
                            }
                            Triple(grid, minimum.takeIf { it.isFinite() } ?: 0f,
                                maximum.takeIf { it.isFinite() } ?: 0f)
                        }
                }
                if (cameraKey == key) {
                    terrain = loaded.first
                    minimumElevation = loaded.second
                    maximumElevation = loaded.third
                    terrainResolution = projection.resolutionPixels
                    val covered = updateMesh(projection, azimuth)
                    map.invalidate()
                    if (!covered && this@MapTerrainRenderer.groundHeight > groundHeight) {
                        loadTerrain(projection, azimuth, key)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e("MapTerrainRenderer", "Unable to load terrain", e)
            }
        }
    }

    fun toMap(point: Vector2): Vector2? = mesh?.toMap(point)

    fun close() {
        scope.cancel()
        bitmap?.recycle()
        bitmap = null
        textureDrawer = null
        terrain = null
    }

    companion object {
        // Must be positive. 1f = native resolution; 0.5f = half each dimension (one quarter
        // of the pixels); 2f = double each dimension (four times the pixels and memory).
        private const val TEXTURE_RESOLUTION_SCALE = 0.5f
    }

}
