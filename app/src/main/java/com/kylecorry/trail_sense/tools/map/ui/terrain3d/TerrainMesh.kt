package com.kylecorry.trail_sense.tools.map.ui.terrain3d

import com.kylecorry.trail_sense.shared.dem.DEM
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Terrain in scene space: x = east, y = up, z = south. 1 unit = 1 km.
 *
 * Vertices are interleaved as position (3), normal (3), texture coordinate (2).
 */
class TerrainMesh(
    val vertices: FloatBuffer,
    val indices: ShortBuffer,
    val indexCount: Int,
    val extent: Float,
    val centerHeight: Float
) {
    companion object {
        private const val KM_PER_DEGREE = 111.32f
        private const val VERTICAL_EXAGGERATION = 2f

        fun from(
            grid: DEM.ElevationGrid,
            centerLatitude: Double,
            centerLongitude: Double,
            toUv: (latitude: Double, longitude: Double) -> Pair<Float, Float>
        ): TerrainMesh {
            val w = grid.width
            val h = grid.height
            val east = FloatArray(w) {
                var delta = grid.longitudes[it] - centerLongitude
                delta = ((delta + 540) % 360) - 180
                (delta * KM_PER_DEGREE * cos(Math.toRadians(centerLatitude))).toFloat()
            }
            val north =
                FloatArray(h) { ((grid.latitudes[it] - centerLatitude) * KM_PER_DEGREE).toFloat() }
            val height = FloatArray(w * h) { grid.elevations[it] / 1000f * VERTICAL_EXAGGERATION }

            val data = ByteBuffer.allocateDirect(w * h * 8 * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val x0 = max(x - 1, 0)
                    val x1 = minOf(x + 1, w - 1)
                    val y0 = max(y - 1, 0)
                    val y1 = minOf(y + 1, h - 1)
                    val dhdE = (height[y * w + x1] - height[y * w + x0]) / (east[x1] - east[x0])
                    val dhdN = (height[y1 * w + x] - height[y0 * w + x]) / (north[y1] - north[y0])
                    // (east, up, north) normal is (-dhdE, 1, -dhdN); scene z is -north
                    val nx = -dhdE
                    val ny = 1f
                    val nz = dhdN
                    val len = sqrt(nx * nx + ny * ny + nz * nz)

                    val (u, v) = toUv(grid.latitudes[y], grid.longitudes[x])
                    data.put(east[x])
                    data.put(height[y * w + x])
                    data.put(-north[y])
                    data.put(nx / len)
                    data.put(ny / len)
                    data.put(nz / len)
                    data.put(u)
                    data.put(v)
                }
            }
            data.position(0)

            val indexData = ByteBuffer.allocateDirect((w - 1) * (h - 1) * 6 * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer()
            for (y in 0 until h - 1) {
                for (x in 0 until w - 1) {
                    val a = (y * w + x).toShort()
                    val b = (y * w + x + 1).toShort()
                    val c = ((y + 1) * w + x).toShort()
                    val d = ((y + 1) * w + x + 1).toShort()
                    indexData.put(a).put(b).put(c)
                    indexData.put(b).put(d).put(c)
                }
            }
            indexData.position(0)

            val extent = max(east.last() - east.first(), north.last() - north.first())
            val centerHeight = height[(h / 2) * w + w / 2]
            return TerrainMesh(data, indexData, (w - 1) * (h - 1) * 6, extent, centerHeight)
        }
    }
}
