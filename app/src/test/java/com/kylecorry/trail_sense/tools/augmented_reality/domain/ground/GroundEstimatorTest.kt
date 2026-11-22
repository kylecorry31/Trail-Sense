package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.sqrt

class GroundEstimatorTest {
    private val w = 160
    private val h = 120
    private val f = w / 2f / tan(Math.toRadians(30.0)).toFloat()

    // rotation = [yaw radians (clockwise from north), pitch radians (down positive)]
    private val projector = object : GroundEstimator.Projector {
        private fun basis(r: FloatArray): Triple<Vector3, Vector3, Vector3> {
            val yaw = r[0]
            val pitch = r[1]
            val fwdH = Vector3(sin(yaw), cos(yaw), 0f)
            val forward = Vector3(fwdH.x * cos(pitch), fwdH.y * cos(pitch), -sin(pitch))
            val right = Vector3(cos(yaw), -sin(yaw), 0f)
            val down = Vector3(
                -fwdH.x * sin(pitch), -fwdH.y * sin(pitch), -cos(pitch)
            ).let { Vector3(-it.x, -it.y, -it.z) }
            return Triple(forward, right, down)
        }

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

    private fun hash(i: Int, j: Int): Float {
        var n = i * 374761393 + j * 668265263
        n = (n xor (n shr 13)) * 1274126177
        return ((n xor (n shr 16)) and 0xffff) / 65535f
    }

    private fun texture(u: Float, v: Float): Float {
        val s = 6f
        val x = u * s
        val y = v * s
        val i = floor(x).toInt()
        val j = floor(y).toInt()
        val fx = x - i
        val fy = y - j
        val a = hash(i, j) * (1 - fx) + hash(i + 1, j) * fx
        val b = hash(i, j + 1) * (1 - fx) + hash(i + 1, j + 1) * fx
        return a * (1 - fy) + b * fy
    }

    private fun render(cam: Vector3, rot: FloatArray, groundZ: Float, slope: Float = 0f): GroundEstimator.Frame {
        val g = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val d = projector.ray(x.toFloat(), y.toFloat(), rot)
            val denom = d.z - slope * d.y
            val t = (slope * cam.y + groundZ - cam.z) / denom
            g[y * w + x] = if (denom < -0.01f && t > 0f) {
                (texture(cam.x + d.x * t, cam.y + d.y * t) * 255).toInt()
            } else 128
        }
        return GroundEstimator.Frame(g, w, h, rot)
    }

    private fun run(height: Float, yaw: Float, pitch: Float, drift: Float = 0f): GroundEstimator.Result {
        val rot = floatArrayOf(yaw, pitch)
        val move = Vector3(sin(yaw), cos(yaw), 0f) * 0.5f
        val a = render(Vector3(0f, 0f, height), rot, 0f)
        val rotB = floatArrayOf(yaw + drift, pitch)
        val b = render(Vector3(move.x, move.y, height), rotB, 0f)
        return GroundEstimator(projector).estimate(a, b, move)
    }

    @Test
    fun recoversCameraHeightAboveFlatGround() {
        for (height in listOf(1.2f, 1.5f, 1.8f)) {
            val result = run(height, 0.4f, Math.toRadians(35.0).toFloat())
            assertNotNull(result.groundDown)
            assertEquals(height, result.groundDown!!, 0.15f)
        }
    }

    @Test
    fun recoversGroundWithSmallOrientationError() {
        val result = run(1.5f, 0.4f, Math.toRadians(35.0).toFloat(), Math.toRadians(0.3).toFloat())
        assertNotNull(result.groundDown)
        assertEquals(1.5f, result.groundDown!!, 0.3f)
    }

    @Test
    fun terrainMapSeesUphillGroundAhead() {
        val slope = 0.2f // rises 1 m per 5 m north
        val pitch = Math.toRadians(30.0).toFloat()
        val rot = floatArrayOf(0f, pitch)
        val map = TerrainMap()
        var y = 0f
        repeat(8) {
            val move = Vector3(0f, 0.8f, 0f)
            val a = render(Vector3(0f, y, 1.5f), rot, 0f, slope)
            val b = render(Vector3(0f, y + 0.8f, 1.5f), rot, 0f, slope)
            val result = GroundEstimator(projector).estimate(a, b, move)
            y += 0.8f
            map.add(result.points, 0f, y)
        }
        // Camera is 1.5 m above ground at y=0 on the slope. Ground at north n is slope*n m up.
        for (n in listOf(2.5f, 4f)) {
            val expected = slope * n - 1.5f
            val actual = map.heightAt(0f, n)
            assertNotNull(actual)
            println("terrain n=$n expected=$expected actual=$actual")
            assertEquals(expected, actual!!, 0.5f)
        }
    }
}
