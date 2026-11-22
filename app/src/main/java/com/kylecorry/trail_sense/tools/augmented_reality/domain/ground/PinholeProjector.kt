package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.tan

/**
 * A pinhole camera for frame pixels. Rotation matrices are the AR view's: OpenGL column major,
 * East-North-Up to camera with the camera's y and z axes swapped.
 */
class PinholeProjector : CameraProjector {

    private class Intrinsics(val fx: Float, val fy: Float, val cx: Float, val cy: Float)

    @Volatile
    private var intrinsics = Intrinsics(1f, 1f, 1f, 1f)

    fun update(width: Int, height: Int, horizontalFov: Float, verticalFov: Float) {
        intrinsics = Intrinsics(
            width / 2f / tan(Math.toRadians(horizontalFov / 2.0)).toFloat(),
            height / 2f / tan(Math.toRadians(verticalFov / 2.0)).toFloat(),
            width / 2f,
            height / 2f
        )
    }

    override fun ray(x: Float, y: Float, rotation: FloatArray): Vector3 {
        val k = intrinsics
        val v = toWorld(Vector3((x - k.cx) / k.fx, -(y - k.cy) / k.fy, 1f), rotation)
        return v * (1f / v.magnitude())
    }

    override fun pixel(ray: Vector3, rotation: FloatArray): Pair<Float, Float>? {
        val k = intrinsics
        val camera = toCamera(ray, rotation)
        if (camera.z <= MIN_DEPTH) return null
        return (k.cx + k.fx * camera.x / camera.z) to (k.cy - k.fy * camera.y / camera.z)
    }

    companion object {
        private const val MIN_DEPTH = 0.01f

        /** Camera axes (x right, y up, z forward) to East-North-Up */
        fun toWorld(camera: Vector3, rotation: FloatArray): Vector3 {
            val w0 = camera.x
            val w1 = camera.z
            val w2 = camera.y
            return Vector3(
                rotation[0] * w0 + rotation[1] * w1 + rotation[2] * w2,
                rotation[4] * w0 + rotation[5] * w1 + rotation[6] * w2,
                rotation[8] * w0 + rotation[9] * w1 + rotation[10] * w2
            )
        }

        private fun toCamera(world: Vector3, rotation: FloatArray): Vector3 {
            val m0 = rotation[0] * world.x + rotation[4] * world.y + rotation[8] * world.z
            val m1 = rotation[1] * world.x + rotation[5] * world.y + rotation[9] * world.z
            val m2 = rotation[2] * world.x + rotation[6] * world.y + rotation[10] * world.z
            return Vector3(m0, m2, m1)
        }
    }
}
