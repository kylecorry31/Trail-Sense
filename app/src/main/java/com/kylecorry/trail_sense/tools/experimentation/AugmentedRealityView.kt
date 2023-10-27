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

    // TODO: Is there a better way to do this - like a rotation matrix or something?
    private val orientation: Quaternion
        get() = Quaternion.from(Euler(inclination, -sideInclination, -azimuth))

    var points = listOf<Point>()

    private val horizon = Path()


    override fun setup() {
    }

    override fun draw() {
        clear()

        // TODO: Extract to layers
        drawNorth()
        drawHorizon()
        drawPoints()
    }

    private fun drawNorth() {
        val north = Path()

        for (i in -90..90 step 5) {
            val pixel = toPixel(HorizonCoordinate(0f, i.toFloat()))
            if (i == -90) {
                north.moveTo(pixel.x, pixel.y)
            } else {
                north.lineTo(pixel.x, pixel.y)
            }
        }

        noFill()
        stroke(Color.WHITE)
        strokeWeight(2f)
        path(north)
        noStroke()
    }

    private fun drawPoints() {
        noStroke()
        points.forEach {
            val pixel = toPixel(it.coordinate)
            fill(it.color)
            circle(pixel.x, pixel.y, sizeToPixel(it.size))
        }
    }

    private fun drawHorizon() {
        horizon.reset()
        var horizonPathStarted = false

        val minAngle = (azimuth - fov.width).toInt()
        val maxAngle = (azimuth + fov.width).toInt()

        for (i in minAngle..maxAngle step 5) {
            val pixel = toPixel(HorizonCoordinate(i.toFloat(), 0f))
            if (!horizonPathStarted) {
                horizon.moveTo(pixel.x, pixel.y)
                horizonPathStarted = true
            } else {
                horizon.lineTo(pixel.x, pixel.y)
            }
        }

        noFill()
        stroke(Color.WHITE)
        strokeWeight(2f)
        path(horizon)
        noStroke()
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
        val r = vector.magnitude()
        val theta = asin(vector.z / r).toDegrees().real(0f)
        val phi = atan2(vector.x, vector.y).toDegrees().real(0f)
        return Vector3(r, theta, phi)
    }

    /**
     * Converts an angular size to a pixel size
     * @param angularSize The angular size in degrees
     * @return The pixel size
     */
    fun sizeToPixel(angularSize: Float): Float {
        return (width / fov.width) * angularSize
    }

    // TODO: These are off by a about a degree when you point the device at around 45 degrees (ex. a north line appears 1 degree to the side of actual north)
    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth.
     * @param bearing The compass bearing in degrees of the point
     * @param elevation The elevation in degrees of the point
     * @return The pixel coordinate of the point
     */
    fun toPixel(coordinate: HorizonCoordinate): PixelCoordinate {
        val world = toWorldSpace(coordinate.bearing, coordinate.elevation, 1f)
        val rotated = applyRotation(world)
        val spherical = toSpherical(rotated)
        // The rotation of the device has been negated, so azimuth = 0 and inclination = 0 is used
        return AugmentedRealityUtils.getPixelLinear(
            spherical.z,
            0f,
            spherical.y,
            0f,
            Size(width.toFloat(), height.toFloat()),
            fov
        )
    }

    data class HorizonCoordinate(val bearing: Float, val elevation: Float)

    data class Point(val coordinate: HorizonCoordinate, val size: Float, val color: Int)

}