package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import kotlin.math.hypot

class ARSize private constructor(
    private val angularSize: Float?,
    actualSize: Distance?,
    private val location: Coordinate?,
    private val elevation: Float?
) {

    private val actualMeters = actualSize?.meters()?.distance

    fun getAngularSize(myLocation: Coordinate, myElevation: Float): Float {
        if (actualMeters != null && location != null) {
            val distance = hypot(
                myLocation.distanceTo(location),
                (elevation ?: myElevation) - myElevation
            )
            return AugmentedRealityUtils.getAngularSize(actualMeters, distance)
        }
        return angularSize ?: 1f
    }

    companion object {
        fun angular(angularSize: Float): ARSize {
            return ARSize(angularSize, null, null, null)
        }

        fun geographic(size: Distance, location: Coordinate, elevation: Float? = null): ARSize {
            return ARSize(null, size, location, elevation)
        }
    }

}