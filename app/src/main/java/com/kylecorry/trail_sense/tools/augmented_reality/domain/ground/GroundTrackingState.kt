package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3

enum class GroundTrackingStatus {
    Searching,
    Tracking,
    Lost
}

/**
 * @param groundOffset the ground's height relative to the camera in meters (negative is below)
 * @param position the camera's position in [map]'s frame, in meters
 * @param velocity the camera's velocity in meters per second
 * @param time when the position was measured, in nanoseconds
 */
class GroundTrackingState(
    val status: GroundTrackingStatus,
    val map: OccupancyGrid,
    val groundOffset: Float?,
    val position: Vector3?,
    val velocity: Vector3,
    val time: Long
)
