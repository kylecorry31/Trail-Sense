package com.kylecorry.trail_sense.shared.views.camera

import android.graphics.Rect
import android.graphics.RectF
import com.kylecorry.andromeda.camera.ar.CameraAnglePixelMapper
import com.kylecorry.andromeda.camera.ar.LinearCameraAnglePixelMapper
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.cos
import kotlin.math.sin

class CalibratedCameraAnglePixelMapper(
    private val camera: Camera,
    private val fallback: CameraAnglePixelMapper = LinearCameraAnglePixelMapper()
) : CameraAnglePixelMapper {

    private var calibration: FloatArray? = null
    private var activeArray: Rect? = null
    private var sensorRotation: Int? = null
    private val linear = LinearCameraAnglePixelMapper()

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
            calibration = camera.getCalibration()
        }
        return calibration
    }

    private fun getActiveArraySize(): Rect? {
        if (activeArray == null) {
            activeArray = camera.getActiveArraySize()
        }
        return activeArray
    }

    private fun getSensorRotation(): Int? {
        if (sensorRotation == null) {
            sensorRotation = camera.sensorRotation.toInt()
        }
        return sensorRotation
    }

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: RectF,
        fieldOfView: Size,
        distance: Float?
    ): PixelCoordinate {
        val world = toCartesian(angleX, angleY, distance ?: 1f)

        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return linear.getPixel(angleX, angleY, imageRect, fieldOfView, distance)
        }

        val sensorRotation = getSensorRotation()
        val calibration = getCalibration()
        val activeArray = getActiveArraySize()

        if (sensorRotation == null || calibration == null || activeArray == null) {
            return fallback.getPixel(angleX, angleY, imageRect, fieldOfView, distance)
        }

        val zoom = camera.zoom?.ratio?.coerceAtLeast(0.05f) ?: 1f
        val unzoomedImageRect = RectF(
            imageRect.centerX() - zoom * imageRect.width() / 2f,
            imageRect.centerY() - zoom * imageRect.height() / 2f,
            imageRect.centerX() + zoom * imageRect.width() / 2f,
            imageRect.centerY() + zoom * imageRect.height() / 2f
        )

        val fx = calibration[0]
        val fy = calibration[1]
        val cx = calibration[2]
        val cy = calibration[3]

        val realFx = if (sensorRotation == 90 || sensorRotation == 270) fx else fy
        val realFy = if (sensorRotation == 90 || sensorRotation == 270) fy else fx
        val realCx = if (sensorRotation == 90 || sensorRotation == 270) cy else cx
        val realCy = if (sensorRotation == 90 || sensorRotation == 270) cx else cy
        val realActiveArray = if (sensorRotation == 90 || sensorRotation == 270) {
            Rect(activeArray.top, activeArray.left, activeArray.bottom, activeArray.right)
        } else {
            activeArray
        }

        val x = realFx * (world.x / world.z) + realCx
        val y = realFy * (world.y / world.z) + realCy

        val invertedY = realActiveArray.height() - y

        val pctX = x / realActiveArray.width()
        val pctY = invertedY / realActiveArray.height()

        val pixelX = pctX * unzoomedImageRect.width() + unzoomedImageRect.left
        val pixelY = pctY * unzoomedImageRect.height() + unzoomedImageRect.top

        return PixelCoordinate(pixelX, pixelY)
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