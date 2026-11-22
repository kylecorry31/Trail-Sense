package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3

/** Converts between frame pixels and world (East-North-Up) rays for a device rotation */
interface CameraProjector {
    /** Unit ray through a frame pixel */
    fun ray(x: Float, y: Float, rotation: FloatArray): Vector3

    /** Frame pixel (x, y) of a ray, or null if it is behind the camera */
    fun pixel(ray: Vector3, rotation: FloatArray): Pair<Float, Float>?
}
