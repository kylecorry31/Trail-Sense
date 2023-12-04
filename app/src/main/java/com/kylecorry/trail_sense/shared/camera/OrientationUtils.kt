package com.kylecorry.trail_sense.shared.camera

import android.hardware.SensorManager
import android.opengl.Matrix
import android.view.Surface
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.SolMath.toDegrees

object OrientationUtils {

    /**
     * Computes the orientation of the device in compass space.
     * @param orientationSensor The orientation sensor
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param surfaceRotation The surface rotation of the device (default Surface.ROTATION_0)
     * @param declination The declination to use (default null)
     */
    fun getCompassOrientation(
        orientationSensor: IOrientationSensor,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        surfaceRotation: Int = Surface.ROTATION_0,
        declination: Float? = null
    ) {
        val x = when (surfaceRotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_X
            Surface.ROTATION_90 -> SensorManager.AXIS_Y
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
            else -> SensorManager.AXIS_X
        }

        val y = when (surfaceRotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_Y
            Surface.ROTATION_90 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_X
            else -> SensorManager.AXIS_Y
        }

        getOrientation(
            orientationSensor,
            x,
            y,
            rotationMatrix,
            orientation,
            declination
        )
    }

    /**
     * Computes the orientation of the device in AR space.
     * @param orientationSensor The orientation sensor
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param declination The declination to use (default null)
     */
    fun getAROrientation(
        orientationSensor: IOrientationSensor,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        declination: Float? = null
    ) {
        getOrientation(
            orientationSensor,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            rotationMatrix,
            orientation,
            declination
        )

        // Not sure if this is actually needed
        orientation[1] = -orientation[1]
        orientation[2] = -orientation[2]
    }

    /**
     * Computes the orientation of the device in the desired coordinate system.
     * @param orientationSensor The orientation sensor
     * @param xAxis The x axis of the coordinate system
     * @param yAxis The y axis of the coordinate system
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param declination The declination to use (default null)
     */
    fun getOrientation(
        orientationSensor: IOrientationSensor,
        xAxis: Int,
        yAxis: Int,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        declination: Float? = null
    ) {
        // Convert the orientation a rotation matrix
        SensorManager.getRotationMatrixFromVector(rotationMatrix, orientationSensor.rawOrientation)

        // Remap the coordinate system if needed
        if (xAxis != SensorManager.AXIS_X || yAxis != SensorManager.AXIS_Y) {
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                xAxis,
                yAxis,
                rotationMatrix
            )
        }

        // Add declination
        if (declination != null) {
            Matrix.rotateM(rotationMatrix, 0, declination, 0f, 0f, 1f)
        }

        // Get orientation from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientation)
        orientation[0] = orientation[0].toDegrees()
        orientation[1] = orientation[1].toDegrees()
        orientation[2] = orientation[2].toDegrees()
    }

}