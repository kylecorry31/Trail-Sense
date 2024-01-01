package com.kylecorry.trail_sense.tools.augmented_reality.mapper

import android.graphics.Rect
import android.graphics.RectF
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A camera angle pixel mapper that uses the intrinsic calibration of the camera to map angles to pixels.
 * @param camera The camera to use
 * @param fallback The fallback mapper to use if the camera does not have intrinsic calibration
 * @param applyDistortionCorrection Whether to apply distortion correction. Defaults to false.
 */
class CalibratedCameraAnglePixelMapper(
    private val camera: ICamera,
    private val fallback: CameraAnglePixelMapper = LinearCameraAnglePixelMapper(),
    private val applyDistortionCorrection: Boolean = false
) : CameraAnglePixelMapper {

    private var calibration: FloatArray? = null
    private var preActiveArray: Rect? = null
    private var activeArray: Rect? = null
    private var distortion: FloatArray? = null
    private val linear = LinearCameraAnglePixelMapper()

    private var zoom: Float = 1f
    private var lastZoomTime = 0L
    private val zoomRefreshInterval = 20L

    override fun getAngle(
        x: Float,
        y: Float,
        imageRect: RectF,
        fieldOfView: Size
    ): Vector2 {
        // TODO: Figure out how to do this
        return fallback.getAngle(x, y, imageRect, fieldOfView)
    }

    private fun getCalibration(): FloatArray? {
        if (calibration == null) {
            val calibration = camera.getIntrinsicCalibration(true) ?: return null
            val rotation = camera.sensorRotation.toInt()
            this.calibration = if (rotation == 90 || rotation == 270) {
                floatArrayOf(
                    calibration[1],
                    calibration[0],
                    calibration[3],
                    calibration[2],
                    calibration[4]
                )
            } else {
                calibration
            }
        }
        return calibration
    }

    private fun getPreActiveArraySize(): Rect? {
        if (preActiveArray == null) {
            val activeArray = camera.getActiveArraySize(true) ?: return null
            val rotation = camera.sensorRotation.toInt()
            this.preActiveArray = if (rotation == 90 || rotation == 270) {
                Rect(activeArray.top, activeArray.left, activeArray.bottom, activeArray.right)
            } else {
                activeArray
            }
        }
        return preActiveArray
    }

    private fun getActiveArraySize(): Rect? {
        if (activeArray == null) {
            val activeArray = camera.getActiveArraySize(false) ?: return null
            val rotation = camera.sensorRotation.toInt()
            this.activeArray = if (rotation == 90 || rotation == 270) {
                Rect(activeArray.top, activeArray.left, activeArray.bottom, activeArray.right)
            } else {
                activeArray
            }
        }
        return activeArray
    }

    private fun getDistortion(): FloatArray? {
        if (distortion == null) {
            distortion = camera.getDistortionCorrection()
        }
        return distortion
    }

    private fun getZoom(): Float {
        if (System.currentTimeMillis() - lastZoomTime < zoomRefreshInterval) {
            return zoom
        }
        zoom = camera.zoom?.ratio?.coerceAtLeast(0.05f) ?: 1f
        lastZoomTime = System.currentTimeMillis()
        return zoom
    }

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: RectF,
        fieldOfView: Size,
        distance: Float?
    ): PixelCoordinate {
        // TODO: Factor in pose translation (just add it to the world position?)
        val world = toCartesian(angleX, angleY, distance ?: 1f)
        return getPixel(world, imageRect, fieldOfView)
    }

    override fun getPixel(world: Vector3, imageRect: RectF, fieldOfView: Size): PixelCoordinate {
        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return linear.getPixel(world, imageRect, fieldOfView)
        }

        val calibration = getCalibration()
        val preActiveArray = getPreActiveArraySize() ?: getActiveArraySize()
        val activeArray = getActiveArraySize()
        val distortion = getDistortion()

        if (calibration == null || preActiveArray == null || activeArray == null) {
            return fallback.getPixel(world, imageRect, fieldOfView)
        }

        val fx = calibration[0]
        val fy = calibration[1]
        val cx = calibration[2]
        val cy = calibration[3]

        // Get the pixel in the pre-active array
        val preX = fx * (world.x / world.z) + cx
        val preY = fy * (world.y / world.z) + cy

        // Correct for distortion
        val corrected = if (applyDistortionCorrection && distortion != null) {
            undistort(preX, preY, preActiveArray, cx, cy, distortion)
        } else {
            Vector2(preX, preY)
        }

        // Translate to the active array
        val activeX = corrected.x - activeArray.left
        val activeY = corrected.y - activeArray.top

        // The y axis is inverted (TODO: Why?)
        val invertedY = activeArray.height() - activeY

        // Unzoom the output image
        val zoom = getZoom()
        val rectLeft = imageRect.centerX() - zoom * imageRect.width() / 2f
        val rectWidth = zoom * imageRect.width()
        val rectTop = imageRect.centerY() - zoom * imageRect.height() / 2f
        val rectHeight = zoom * imageRect.height()

        // Scale to the output image dimensions
        val pctX = activeX / activeArray.width()
        val pctY = invertedY / activeArray.height()
        val pixelX = pctX * rectWidth + rectLeft
        val pixelY = pctY * rectHeight + rectTop

        return PixelCoordinate(pixelX, pixelY)
    }

    private fun undistort(
        x: Float,
        y: Float,
        activeArray: Rect,
        cx: Float,
        cy: Float,
        distortion: FloatArray
    ): Vector2 {
        val sizeX = max(cx - activeArray.left, activeArray.right - cx)
        val sizeY = max(cy - activeArray.top, activeArray.bottom - cy)

        val normalizedX = (x - cx) / sizeX
        val normalizedY = (y - cy) / sizeY

        val rSquared = normalizedX * normalizedX + normalizedY * normalizedY

        val radialDistortion =
            1 + distortion[0] * rSquared + distortion[1] * rSquared * rSquared + distortion[2] * rSquared * rSquared * rSquared

        val xc =
            normalizedX * radialDistortion + distortion[3] * (2 * normalizedX * normalizedY) + distortion[4] * (rSquared + 2 * normalizedX * normalizedX)
        val yc =
            normalizedY * radialDistortion + distortion[3] * (rSquared + 2 * normalizedX * normalizedY) + distortion[4] * (2 * normalizedY * normalizedY)

        return Vector2(xc * sizeX + cx, yc * sizeY + cy)
    }

    private fun toCartesian(
        bearing: Float,
        altitude: Float,
        radius: Float
    ): Vector3 {
        val altitudeRad = altitude.toRadians()
        val bearingRad = bearing.toRadians()
        val cosAltitude = cos(altitudeRad)
        val sinAltitude = sin(altitudeRad)
        val cosBearing = cos(bearingRad)
        val sinBearing = sin(bearingRad)

        // X and Y are flipped
        val x = sinBearing * cosAltitude * radius
        val y = cosBearing * sinAltitude * radius
        val z = cosBearing * cosAltitude * radius
        return Vector3(x, y, z)
    }
}