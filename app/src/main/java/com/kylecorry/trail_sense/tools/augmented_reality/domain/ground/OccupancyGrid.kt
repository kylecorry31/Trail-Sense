package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs
import kotlin.math.floor

/**
 * A voxel map of the world around the user, in meters with z up. Each observed point makes its
 * voxel more likely occupied and the voxels along its line of sight more likely empty, so noise and
 * things that moved fade away. Each tracked feature votes once per voxel, so it takes two features
 * agreeing to occupy one.
 */
class OccupancyGrid(val voxelSize: Float = 0.3f) {

    class Voxel(val x: Int, val y: Int, val z: Int)

    private val logOdds = HashMap<Long, Float>()
    private val votes = HashMap<Long, HashSet<Long>>()

    /**
     * @param points the tracked features by ID
     */
    fun add(camera: Vector3, points: Map<Long, Vector3>) {
        synchronized(logOdds) {
            votes.keys.retainAll(points.keys)
            for ((id, p) in points) {
                val target = index(p)
                val voted = votes.getOrPut(id) { HashSet() }
                if (!voted.add(pack(target.first, target.second, target.third))) continue
                carve(camera, p, target)
                update(target, HIT)
            }
        }
    }

    fun occupied(): List<Voxel> = synchronized(logOdds) {
        logOdds.mapNotNull { (k, v) ->
            if (v < OCCUPIED) null else {
                val (x, y, z) = unpack(k)
                Voxel(x, y, z)
            }
        }
    }

    fun center(voxel: Voxel): Vector3 {
        return Vector3((voxel.x + 0.5f) * voxelSize, (voxel.y + 0.5f) * voxelSize, (voxel.z + 0.5f) * voxelSize)
    }

    fun clear() {
        synchronized(logOdds) {
            logOdds.clear()
            votes.clear()
        }
    }

    // 3D DDA from the camera to the voxel before the point
    private fun carve(from: Vector3, to: Vector3, target: Triple<Int, Int, Int>) {
        val start = floatArrayOf(from.x, from.y, from.z)
        val direction = (to - from).let { floatArrayOf(it.x, it.y, it.z) }
        val (ix, iy, iz) = index(from)
        val voxel = intArrayOf(ix, iy, iz)
        val step = IntArray(3) { if (direction[it] > 0) 1 else -1 }
        val tMax = FloatArray(3) {
            val edge = (if (step[it] > 0) voxel[it] + 1 else voxel[it]) * voxelSize
            if (abs(direction[it]) < MIN_DIRECTION) Float.MAX_VALUE else (edge - start[it]) / direction[it]
        }
        val tDelta = FloatArray(3) {
            if (abs(direction[it]) < MIN_DIRECTION) Float.MAX_VALUE else voxelSize / abs(direction[it])
        }
        repeat(MAX_STEPS) {
            val current = Triple(voxel[0], voxel[1], voxel[2])
            if (current == target) return
            update(current, MISS)
            val axis = (0..2).minBy { tMax[it] }
            // Past t = 1 is the point itself
            if (tMax[axis] > 1f) return
            voxel[axis] += step[axis]
            tMax[axis] += tDelta[axis]
        }
    }

    private fun update(index: Triple<Int, Int, Int>, delta: Float) {
        val key = pack(index.first, index.second, index.third)
        val value = ((logOdds[key] ?: 0f) + delta).coerceIn(MIN_LOG_ODDS, MAX_LOG_ODDS)
        if (value <= MIN_LOG_ODDS && delta < 0) logOdds.remove(key) else logOdds[key] = value
    }

    private fun index(p: Vector3) = Triple(
        floor(p.x / voxelSize).toInt(),
        floor(p.y / voxelSize).toInt(),
        floor(p.z / voxelSize).toInt()
    )

    private fun pack(x: Int, y: Int, z: Int): Long {
        return ((x + OFFSET).toLong() shl 42) or ((y + OFFSET).toLong() shl 21) or (z + OFFSET).toLong()
    }

    private fun unpack(key: Long): Triple<Int, Int, Int> {
        val mask = (1L shl 21) - 1
        return Triple(
            ((key shr 42) and mask).toInt() - OFFSET,
            ((key shr 21) and mask).toInt() - OFFSET,
            (key and mask).toInt() - OFFSET
        )
    }

    private companion object {
        const val HIT = 0.85f
        const val MISS = -0.4f
        const val OCCUPIED = 1.2f // needs at least two hits
        const val MIN_LOG_ODDS = -2f
        const val MAX_LOG_ODDS = 3.5f
        const val MAX_STEPS = 200
        const val MIN_DIRECTION = 1e-6f
        const val OFFSET = 1 shl 20
    }
}
