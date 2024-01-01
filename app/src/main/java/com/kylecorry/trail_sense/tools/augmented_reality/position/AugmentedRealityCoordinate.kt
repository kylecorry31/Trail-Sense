package com.kylecorry.trail_sense.tools.augmented_reality.position

import com.kylecorry.sol.math.Vector3

/**
 * A point in the AR world
 * @param position The position of the point in East, North, Up coordinates
 * @param isTrueNorth True if the reference frame is true north, false if it is magnetic north
 */
data class AugmentedRealityCoordinate(val position: Vector3, val isTrueNorth: Boolean = true)