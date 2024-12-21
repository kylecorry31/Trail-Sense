package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import android.graphics.Rect
import android.graphics.RectF
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.optics.Optics
import kotlin.math.max

/**
 * A camera angle pixel mapper that uses the intrinsic calibration of the camera to map angles to pixels.
 * @param camera The camera to use
 * @param fallback The fallback mapper to use if the camera does not have intrinsic calibration
 * @param applyDistortionCorrection Whether to apply distortion correction. Defaults to false.
 */
class CalibratedCameraAnglePixelMapper(
    private val camera: ICamera,
    private val fallback: CameraAnglePixelMapper = LinearCameraAnglePixelMapper(),
    private val applyDistortionCorrection: Boolean = false,
    private val useManufacturerCalibration: Boolean = false
) : CameraAnglePixelMapper {

    private var focalLength: Vector2? = null
    private var opticalCenter: Vector2? = null
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
        val focalLength = getFocalLength()
        val opticalCenter = getOpticalCenter()
        val preActiveArray = getPreActiveArraySize() ?: getActiveArraySize()
        val activeArray = getActiveArraySize()

        if (focalLength == null || opticalCenter == null || preActiveArray == null || activeArray == null) {
            return fallback.getAngle(x, y, imageRect, fieldOfView)
        }

        val zoom = getZoom()
        val rectLeft = imageRect.centerX() - zoom * imageRect.width() / 2f
        val rectWidth = zoom * imageRect.width()
        val rectTop = imageRect.centerY() - zoom * imageRect.height() / 2f
        val rectHeight = zoom * imageRect.height()

        val pctX = (x - rectLeft) / rectWidth
        val pctY = (y - rectTop) / rectHeight
        val activeX = pctX * activeArray.width()
        val invertedY = pctY * activeArray.height()
        val activeY = activeArray.height() - invertedY

        val correctedX = activeX + activeArray.left
        val correctedY = activeY + activeArray.top

        // TODO: Distort
        val pre = Vector2(correctedX, correctedY)

        val world = Optics.inversePerspectiveProjection(pre, focalLength, opticalCenter)

        val spherical = CameraAnglePixelMapper.toSpherical(world)

        return Vector2(spherical.z, spherical.y)
    }

    private fun updateCalibration() {
        if (focalLength == null || opticalCenter == null) {
            val calibration =
                camera.getIntrinsicCalibration(true, onlyUseEstimated = !useManufacturerCalibration)
                    ?: return
            val rotation = camera.sensorRotation.toInt()
            if (rotation == 90 || rotation == 270) {
                focalLength = Vector2(calibration[1], calibration[0])
                opticalCenter = Vector2(calibration[3], calibration[2])
            } else {
                focalLength = Vector2(calibration[0], calibration[1])
                opticalCenter = Vector2(calibration[2], calibration[3])
            }
        }
    }

    private fun getFocalLength(): Vector2? {
        if (focalLength == null) {
            updateCalibration()
        }
        return focalLength
    }

    private fun getOpticalCenter(): Vector2? {
        if (opticalCenter == null) {
            updateCalibration()
        }
        return opticalCenter
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
        val world = CameraAnglePixelMapper.toCartesian(angleX, angleY, distance ?: 1f)
        return getPixel(world, imageRect, fieldOfView)
    }

    override fun getPixel(world: Vector3, imageRect: RectF, fieldOfView: Size): PixelCoordinate {
        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return linear.getPixel(world, imageRect, fieldOfView)
        }

        val focalLength = getFocalLength()
        val opticalCenter = getOpticalCenter()
        val preActiveArray = getPreActiveArraySize() ?: getActiveArraySize()
        val activeArray = getActiveArraySize()
        val distortion = getDistortion()

        if (focalLength == null || opticalCenter == null || preActiveArray == null || activeArray == null) {
            return fallback.getPixel(world, imageRect, fieldOfView)
        }

        // Get the pixel in the pre-active array
        val pre = Optics.perspectiveProjection(world, focalLength, opticalCenter)

        // Correct for distortion
        val corrected = if (applyDistortionCorrection && distortion != null) {
            undistort(pre.x, pre.y, preActiveArray, opticalCenter.x, opticalCenter.y, distortion)
        } else {
            Vector2(pre.x, pre.y)
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
}