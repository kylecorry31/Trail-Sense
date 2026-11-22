package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * A coarse height grid of the terrain around the user, built from triangulated points.
 * Heights are meters up relative to the camera (negative is below it). Positions are in a local
 * East/North frame that starts where tracking started.
 */
class TerrainMap {
    private val heights = HashMap<Long, Float>()

    @Volatile
    var version = 0
        private set

    fun clear() {
        synchronized(heights) { heights.clear() }
        version++
    }

    /** @param origin the camera position (east, north) the points are relative to */
    fun add(points: List<Vector3>, originX: Float, originY: Float) {
        val batch = HashMap<Long, MutableList<Float>>()
        for (p in points) {
            if (p.z > MAX_UP || p.z < -MAX_DOWN) continue
            batch.getOrPut(key(cell(originX + p.x), cell(originY + p.y))) { mutableListOf() }.add(p.z)
        }
        synchronized(heights) {
            for ((k, zs) in batch) {
                if (zs.size < MIN_POINTS) continue
                zs.sort()
                val median = zs[zs.size / 2]
                val old = heights[k]
                heights[k] = if (old == null) median else old + (median - old) * ALPHA
            }
        }
        version++
    }

    fun heightAt(x: Float, y: Float): Float? {
        val cx = cell(x)
        val cy = cell(y)
        var sum = 0f
        var weight = 0f
        synchronized(heights) {
            for (dy in -RADIUS..RADIUS) for (dx in -RADIUS..RADIUS) {
                val h = heights[key(cx + dx, cy + dy)] ?: continue
                val w = 1f / (1f + abs(dx) + abs(dy))
                sum += h * w
                weight += w
            }
        }
        return if (weight == 0f) null else sum / weight
    }

    private fun cell(v: Float) = floor(v / CELL).toInt()
    private fun key(x: Int, y: Int) = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)

    private companion object {
        const val CELL = 1f
        const val RADIUS = 2
        const val MIN_POINTS = 3
        const val ALPHA = 0.35f
        const val MAX_UP = 0.5f
        const val MAX_DOWN = 4f
    }
}
