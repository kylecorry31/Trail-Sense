package com.kylecorry.trail_sense.shared.camera

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toRadians
import kotlin.math.absoluteValue
import kotlin.math.sin

object AugmentedRealityUtils {

    /**
     * Gets the x coordinate of a point on the screen given the bearing and azimuth.
     * @param bearing The compass bearing in degrees of the point
     * @param azimuth The compass bearing in degrees that the user is facing (center of the screen)
     * @param range The visible range of the compass in degrees / horizontal FOV
     * @param width The width of the view
     * @param is3D True if the point should be rendered onto a 3D sphere
     */
    fun getX(
        bearing: Float,
        azimuth: Float,
        range: Float,
        width: Float,
        is3D: Boolean = true
    ): Float {
        val delta = SolMath.deltaAngle(azimuth, bearing)

        val x = if (is3D && delta.absoluteValue < range / 2f) {
            // TODO: Factor in altitude / distance and use spherical trigonometry
            val radius = width / (sin((range / 2f).toRadians()) * 2f)
            sin(delta.toRadians()) * radius
        } else {
            val pixelsPerDegree = width / range
            delta * pixelsPerDegree
        }

        return width / 2f + x
    }

}