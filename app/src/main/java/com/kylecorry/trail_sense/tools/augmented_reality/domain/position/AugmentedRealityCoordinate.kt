package com.kylecorry.trail_sense.tools.augmented_reality.domain.position

import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/**
 * A point in the AR world
 * @param position The position of the point in East, North, Up coordinates
 * @param isTrueNorth True if the reference frame is true north, false if it is magnetic north
 */
data class AugmentedRealityCoordinate(val position: Vector3, val isTrueNorth: Boolean = true) {

    val bearing: Float
        get() {
            return atan2(position.x, position.y).toDegrees().real(0f)
        }

    // The bearing doesn't exist if the point is directly above or below
    val hasBearing: Boolean
        get() = abs(position.x) > 0.001f || abs(position.y) > 0.001f

    val elevation: Float
        get() = asin(position.z / distance).toDegrees().real(0f)

    val distance: Float
        get() = position.magnitude()

    companion object {
        fun fromSpherical(
            bearing: Float,
            elevation: Float,
            distance: Float,
            isTrueNorth: Boolean = true
        ): AugmentedRealityCoordinate {
            return AugmentedRealityCoordinate(
                AugmentedRealityUtils.toEastNorthUp(bearing, elevation, distance),
                isTrueNorth
            )
        }
    }

}