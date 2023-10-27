package com.kylecorry.trail_sense.tools.experimentation

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class AugmentedRealityView : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var fov: Size = Size(45f, 45f)
    var azimuth = 0f
    var inclination = 0f
    var sideInclination = 0f

    // TODO: Is there a better way to do this?
    val orientation: Quaternion
        get() = Quaternion.from(Euler(inclination, -sideInclination, -azimuth))

    var points = listOf<Point>()

    override fun setup() {
    }

    override fun draw() {
        push()
        clear()

        // TODO: Figure out why this is drawing an extra line
        val horizonPath = Path()
        for (i in 0..360 step 5) {
            val pixel = getPixel(i.toFloat(), 0f)
            if (i == 0) {
                horizonPath.moveTo(pixel.x, pixel.y)
            } else {
                horizonPath.lineTo(pixel.x, pixel.y)
            }
        }
        horizonPath.close()

        noFill()
        stroke(Color.WHITE)
        strokeWeight(2f)
        path(horizonPath)

        noStroke()


        points.forEach {
            val pixel = getPixel(it.bearing, it.elevation)
            fill(it.color)
            circle(pixel.x, pixel.y, getSize(it.size))
        }
        pop()
    }

    private fun toWorldSpace(bearing: Float, elevation: Float, distance: Float): Vector3 {
        val thetaRad = elevation.toRadians()
        val phiRad = bearing.toRadians()

        val cosTheta = cos(thetaRad)
        val x = distance * cosTheta * sin(phiRad)
        val y = distance * cosTheta * cos(phiRad)
        val z = distance * sin(thetaRad)
        return Vector3(x, y, z)
    }

    private fun applyRotation(vector: Vector3): Vector3 {
        return orientation.inverse().rotate(vector)
    }

    private fun toSpherical(vector: Vector3): Vector3 {
        return Vector3(1f, asin(vector.z).toDegrees(), atan2(vector.x, vector.y).toDegrees())
    }

    private fun getSize(angularSize: Float): Float {
        return (width / fov.width) * angularSize
    }

    private fun getPixel(bearing: Float, elevation: Float): PixelCoordinate {
        val world = toWorldSpace(bearing, elevation, 1f)
        val rotated = applyRotation(world)
        val spherical = toSpherical(rotated)
        return AugmentedRealityUtils.getPixelLinear(
            spherical.z,
            0f,
            spherical.y,
            0f,
            Size(width.toFloat(), height.toFloat()),
            fov
        )


//        return AugmentedRealityUtils.getPixelLinear(
//            bearing,
//            azimuth,
//            elevation,
//            inclination,
//            Size(width.toFloat(), height.toFloat()),
//            fov
//        )
    }

    data class Point(val bearing: Float, val elevation: Float, val size: Float, val color: Int)

}