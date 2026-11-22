package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * A pinhole camera over textured ground z = slope * north, which can change to another slope
 * beyond a north distance (like the top of a hill).
 * rotation = [yaw radians (clockwise from north), pitch radians (down positive), roll radians]
 */
class SyntheticScene(
    val w: Int = 160,
    val h: Int = 120,
    hfovDeg: Double = 60.0,
    val slope: Float = 0f,
    private val breakNorth: Float = Float.MAX_VALUE,
    private val slopeAfterBreak: Float = slope
) {
    private val f = w / 2f / tan(Math.toRadians(hfovDeg / 2)).toFloat()

    val projector = object : CameraProjector {
        override fun ray(x: Float, y: Float, rotation: FloatArray): Vector3 {
            val (fwd, right, down) = basis(rotation)
            val v = fwd * f + right * (x - w / 2f) + down * (y - h / 2f)
            return v * (1f / v.magnitude())
        }

        override fun pixel(ray: Vector3, rotation: FloatArray): Pair<Float, Float>? {
            val (fwd, right, down) = basis(rotation)
            val z = ray.dot(fwd)
            if (z <= 0.01f) return null
            return (w / 2f + f * ray.dot(right) / z) to (h / 2f + f * ray.dot(down) / z)
        }
    }

    private fun basis(r: FloatArray): Triple<Vector3, Vector3, Vector3> {
        val yaw = r[0]
        val pitch = r[1]
        val roll = r.getOrElse(2) { 0f }
        val fwdH = Vector3(sin(yaw), cos(yaw), 0f)
        val forward = Vector3(fwdH.x * cos(pitch), fwdH.y * cos(pitch), -sin(pitch))
        val right0 = Vector3(cos(yaw), -sin(yaw), 0f)
        val down0 = Vector3(fwdH.x * sin(pitch), fwdH.y * sin(pitch), cos(pitch)) * -1f
        val down1 = down0 * -1f
        // Roll around the forward axis
        val right = right0 * cos(roll) + down1 * sin(roll)
        val down = down1 * cos(roll) - right0 * sin(roll)
        return Triple(forward, right, down)
    }

    private fun hash(i: Int, j: Int, seed: Int): Float {
        var n = i * 374761393 + j * 668265263 + seed * 144269504
        n = (n xor (n shr 13)) * 1274126177
        return ((n xor (n shr 16)) and 0xffff) / 65535f
    }

    private fun noise(u: Float, v: Float, scale: Float, seed: Int): Float {
        val x = u * scale
        val y = v * scale
        val i = floor(x).toInt()
        val j = floor(y).toInt()
        val fx = x - i
        val fy = y - j
        val a = hash(i, j, seed) * (1 - fx) + hash(i + 1, j, seed) * fx
        val b = hash(i, j + 1, seed) * (1 - fx) + hash(i + 1, j + 1, seed) * fx
        return a * (1 - fy) + b * fy
    }

    // Two octaves: patches of dirt (~17 cm) and gravel (~5 cm)
    private fun texture(u: Float, v: Float): Float {
        return 0.6f * noise(u, v, 6f, 1) + 0.4f * noise(u, v, 20f, 2)
    }

    fun groundZ(north: Float): Float {
        return if (north < breakNorth) slope * north else slope * breakNorth + slopeAfterBreak * (north - breakNorth)
    }

    // Distance along the ray to the ground, or null if it misses
    private fun hit(cam: Vector3, d: Vector3): Float? {
        fun plane(s: Float, c: Float): Float? {
            val denom = d.z - s * d.y
            if (abs(denom) < 1e-6f) return null
            val t = (s * cam.y + c - cam.z) / denom
            return if (t > 0f && t < 200f) t else null
        }
        val first = plane(slope, 0f)
        if (first != null && cam.y + d.y * first < breakNorth) return first
        val second = plane(slopeAfterBreak, (slope - slopeAfterBreak) * breakNorth)
        if (second != null && cam.y + d.y * second >= breakNorth) return second
        return null
    }

    fun render(cam: Vector3, rotation: FloatArray): FeatureTracker.Image {
        val g = IntArray(w * h)
        // 2x2 supersampling, like downscaling a larger camera frame
        for (y in 0 until h) for (x in 0 until w) {
            var sum = 0f
            for (sy in 0..1) for (sx in 0..1) {
                val d = projector.ray(x - 0.25f + sx * 0.5f, y - 0.25f + sy * 0.5f, rotation)
                val t = hit(cam, d)
                sum += if (t != null) texture(cam.x + d.x * t, cam.y + d.y * t) * 255 else 200f
            }
            g[y * w + x] = (sum / 4f).toInt().coerceIn(0, 255)
        }
        return FeatureTracker.Image(FloatArray(g.size) { g[it].toFloat() }, w, h)
    }
}
