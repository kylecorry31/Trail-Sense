package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.science.geology.CoordinateBounds
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.hypot

fun getSlopeVector(
    cellSizeX: Double,
    cellSizeY: Double,
    x: Int,
    y: Int,
    getElevation: (x: Int, y: Int) -> Float
): Vector2 {
    val a = getElevation(x - 1, y - 1)
    val b = getElevation(x, y - 1)
    val c = getElevation(x + 1, y - 1)
    val d = getElevation(x - 1, y)
    val f = getElevation(x + 1, y)
    val g = getElevation(x - 1, y + 1)
    val h = getElevation(x, y + 1)
    val i = getElevation(x + 1, y + 1)

    val dx = (((c + 2 * f + i) - (a + 2 * d + g)) / (8 * cellSizeX)).toFloat()
    val dy = (((g + 2 * h + i) - (a + 2 * b + c)) / (8 * cellSizeY)).toFloat()
    return Vector2(dx, dy)
}

fun getSlopeAngle(
    slopeVector: Vector2,
    zFactor: Float = 1f
): Float {
    return atan(zFactor * hypot(slopeVector.x, slopeVector.y))
}

fun getSlopeAspect(slopeVector: Vector2): Float {
    var aspectRad = 0f
    if (!Arithmetic.isZero(slopeVector.x)) {
        aspectRad = wrap(atan2(slopeVector.y, -slopeVector.x), 0f, 2 * PI.toFloat())
    } else {
        if (slopeVector.y > 0) {
            aspectRad = PI.toFloat() / 2
        } else if (slopeVector.y < 0) {
            aspectRad = 3 * PI.toFloat() / 2
        }
    }
    return aspectRad
}

fun getCellSizeX(resolution: Double, bounds: CoordinateBounds): Double {
    return resolution * 111319.5 * cosDegrees(bounds.center.latitude)
}

fun getCellSizeY(resolution: Double): Double {
    return resolution * 111319.5
}