package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.physics.DragModel
import kotlin.math.abs

abstract class TabulatedDragModel(val bc: Float = 1f) : DragModel {

    abstract val dragTable: Map<Float, Float>

    private val interpolator by lazy { TableInterpolator(dragTable) }


    override fun getDragAcceleration(velocity: Vector2): Vector2 {
        val magnitude = velocity.magnitude()
        val angle = velocity.angle()
        val drag = interpolator.interpolate(magnitude)
        val dragX = drag * SolMath.cosDegrees(angle)
        val dragY = drag * SolMath.sinDegrees(angle)

        // Always in the direction opposite to the velocity
        val xSign = if (velocity.x < 0) 1 else -1
        val ySign = if (velocity.y < 0) 1 else -1

        return Vector2(abs(dragX) * xSign / bc, abs(dragY) * ySign / bc)
    }
}