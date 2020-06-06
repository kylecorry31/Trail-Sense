package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.math.deltaAngle
import com.kylecorry.trail_sense.weather.domain.MovingAverageFilter
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

class CustomOrientationCompass(context: Context) : AbstractSensor(), ICompass {

    private val accelerometer = Accelerometer(context)
    private val magnetometer = Magnetometer(context)

    private val prefs = UserPreferences(context)
    private val filterSize = prefs.navigation.compassSmoothing * 2 * 2
    private val filter = MovingAverageFilter(filterSize)

    override var declination = 0f

    override val bearing: Bearing
        get() = Bearing(_filteredBearing).withDeclination(declination)

    private var _bearing = 0f
    private var _filteredBearing = 0f

    private val m_NormGravityVector = FloatArray(3)
    private val m_NormMagFieldValues = FloatArray(3)
    private var m_Norm_Gravity = 0f
    private var m_Norm_MagField = 0f

    private val m_NormEastVector =
        FloatArray(3)       // normalised cross product of raw gravity vector with magnetic field values, points east
    private val m_NormNorthVector =
        FloatArray(3)      // Normalised vector pointing to magnetic north
    private var m_azimuth_radians = 0f        // angle of the device from magnetic north
    private var m_pitch_radians =
        0f          // tilt angle of the device from the horizontal.  m_pitch_radians = 0 if the device if flat, m_pitch_radians = Math.PI/2 means the device is upright.
    private var m_pitch_axis_radians = 0f

    private var gotMag = false;
    private var gotAccel = false;

    private fun updateBearing(newBearing: Float) {
        _bearing += deltaAngle(_bearing, newBearing)
        _filteredBearing = filter.filter(_bearing.toDouble()).toFloat()
    }


    private fun updateSensor(): Boolean {

        if (!gotAccel || !gotMag){
            return true;
        }

        // Gravity
        System.arraycopy(
            accelerometer.acceleration,
            0,
            m_NormGravityVector,
            0,
            m_NormGravityVector.size
        );
        m_Norm_Gravity =
            sqrt(m_NormGravityVector[0] * m_NormGravityVector[0] + m_NormGravityVector[1] * m_NormGravityVector[1] + m_NormGravityVector[2] * m_NormGravityVector[2])
        for (i in m_NormGravityVector.indices) m_NormGravityVector[i] /= m_Norm_Gravity;

        // Magnetic field
        System.arraycopy(
            magnetometer.magneticField,
            0,
            m_NormMagFieldValues,
            0,
            m_NormMagFieldValues.size
        );
        m_Norm_MagField =
            sqrt(m_NormMagFieldValues[0] * m_NormMagFieldValues[0] + m_NormMagFieldValues[1] * m_NormMagFieldValues[1] + m_NormMagFieldValues[2] * m_NormMagFieldValues[2]);
        for (i in m_NormMagFieldValues.indices) m_NormMagFieldValues[i] /= m_Norm_MagField;


        // first calculate the horizontal vector that points due east
        val East_x =
            m_NormMagFieldValues[1] * m_NormGravityVector[2] - m_NormMagFieldValues[2] * m_NormGravityVector[1];
        val East_y =
            m_NormMagFieldValues[2] * m_NormGravityVector[0] - m_NormMagFieldValues[0] * m_NormGravityVector[2];
        val East_z =
            m_NormMagFieldValues[0] * m_NormGravityVector[1] - m_NormMagFieldValues[1] * m_NormGravityVector[0];
        val norm_East = sqrt(East_x * East_x + East_y * East_y + East_z * East_z);
        if (m_Norm_Gravity * m_Norm_MagField * norm_East < 0.1f) {  // Typical values are  > 100.
            return true
        } else {
            m_NormEastVector[0] = East_x / norm_East; m_NormEastVector[1] =
                East_y / norm_East; m_NormEastVector[2] = East_z / norm_East;

            // next calculate the horizontal vector that points due north
            val M_dot_G =
                (m_NormGravityVector[0] * m_NormMagFieldValues[0] + m_NormGravityVector[1] * m_NormMagFieldValues[1] + m_NormGravityVector[2] * m_NormMagFieldValues[2]);
            val North_x = m_NormMagFieldValues[0] - m_NormGravityVector[0] * M_dot_G;
            val North_y = m_NormMagFieldValues[1] - m_NormGravityVector[1] * M_dot_G;
            val North_z = m_NormMagFieldValues[2] - m_NormGravityVector[2] * M_dot_G;
            val norm_North = sqrt(North_x * North_x + North_y * North_y + North_z * North_z);
            m_NormNorthVector[0] = North_x / norm_North; m_NormNorthVector[1] =
                North_y / norm_North; m_NormNorthVector[2] = North_z / norm_North;

            // take account of screen rotation away from its natural rotation
//                int rotation = m_activity.getWindowManager().getDefaultDisplay().getRotation();
            val screen_adjustment = 0f;
//                switch(rotation) {
//                    case Surface.ROTATION_0:   screen_adjustment =          0;         break;
//                    case Surface.ROTATION_90:  screen_adjustment =   (float)Math.PI/2; break;
//                    case Surface.ROTATION_180: screen_adjustment =   (float)Math.PI;   break;
//                    case Surface.ROTATION_270: screen_adjustment = 3*(float)Math.PI/2; break;
//                }
            // NB: the rotation matrix has now effectively been calculated. It consists of the three vectors m_NormEastVector[], m_NormNorthVector[] and m_NormGravityVector[]

            // calculate all the required angles from the rotation matrix
            // NB: see https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
            var sin = m_NormEastVector[1] - m_NormNorthVector[0];
            var cos = m_NormEastVector[0] + m_NormNorthVector[1];
            m_azimuth_radians = if (sin != 0f && cos != 0f) atan2(sin, cos) else 0f
            m_pitch_radians = acos(m_NormGravityVector[2])
            sin = -m_NormEastVector[1] - m_NormNorthVector[0];
            cos = m_NormEastVector[0] - m_NormNorthVector[1];
            val aximuth_plus_two_pitch_axis_radians =
                if (sin != 0f && cos != 0f) atan2(sin, cos) else 0f
            m_pitch_axis_radians = (aximuth_plus_two_pitch_axis_radians - m_azimuth_radians) / 2f;
            m_azimuth_radians += screen_adjustment;
            m_pitch_axis_radians += screen_adjustment;
        }

        updateBearing(Math.toDegrees(m_azimuth_radians.toDouble()).toFloat())
        notifyListeners()
        return true
    }

    private fun updateAccel(): Boolean {
        gotAccel = true
        return updateSensor()
    }

    private fun updateMag(): Boolean {
        gotMag = true
        return updateSensor()
    }

    override fun startImpl() {
        accelerometer.start(this::updateAccel)
        magnetometer.start(this::updateMag)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateAccel)
        magnetometer.stop(this::updateMag)
    }

}