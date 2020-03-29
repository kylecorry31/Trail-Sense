package com.kylecorry.trail_sense.shared.sensors.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.kylecorry.sensorfilters.ISensorFilter
import com.kylecorry.sensorfilters.KalmanFilter
import com.kylecorry.trail_sense.shared.Bearing
import com.kylecorry.trail_sense.shared.CompassDirection
import com.kylecorry.trail_sense.shared.sensors.ISensor
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

/**
 * A compass sensor
 */
class Compass (ctx: Context) : ICompass, ISensor, SensorEventListener, Observable() {

    private var sensorManager: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelReading = FloatArray(3)
    private val magnetReading = FloatArray(3)

    private val rotation = FloatArray(9)
    private val orientation = FloatArray(3)

    private var totalAngle = 0f

    private var started = false

    private val azimuthKf = KalmanFilter(0.5, 0.0001)

    private val magXKf = KalmanFilter(0.9, 0.001)
    private val magYKf = KalmanFilter(0.9, 0.001)
    private val magZKf = KalmanFilter(0.9, 0.001)

    /**
     * The direction in degrees that the compass is facing (called azimuth)
     */
    override val azimuth: Bearing
        get() {
            updateOrientation()

            if (orientation[0] == 0.0F){
                return Bearing(declination)
            }

            var currentAngle = (Math.toDegrees(orientation[0].toDouble()) + 360) % 360
            totalAngle += deltaAngle(totalAngle, currentAngle.toFloat())

            return Bearing(
                azimuthKf.filter(
                    totalAngle.toDouble()
                ).toFloat() + declination
            )
        }

    /**
     * The cardinal/inter-cardinal direction that the compass is facing
     */
    val direction: CompassDirection
        get(){
            val directions = CompassDirection.values()
            val a = azimuth
            directions.forEach {
                if (closeTo(a.value, it.azimuth, 22.5f)){
                    return it
                }
            }
            return CompassDirection.NORTH
        }

    /**
     * The declination in degrees to apply to the azimuth
     */
    var declination: Float = 0f

    /**
     * Start the compass sensor
     */
    override fun start(){
        if (started) return
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        started = true
    }

    /**
     * Stop the compass sensor
     */
    override fun stop(){
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Empty
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            for (i in event.values.indices){
                accelReading[i] = event.values[i]
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            applySmoothing(event.values, magnetReading, listOf(magXKf, magYKf, magZKf))
        }
        setChanged()
        notifyObservers()
    }

    // Private helpers

    /**
     * Apply smoothing to an array
     */
    private fun applySmoothing(input: FloatArray, output: FloatArray, filters: List<ISensorFilter>) {
        if (output.size != input.size) return
        for (i in input.indices){
            output[i] = filters[i].filter(input[i].toDouble()).toFloat()
        }
    }

    /**
     * Update the orientation values
     */
    private fun updateOrientation(){
        SensorManager.getRotationMatrix(rotation, null, accelReading, magnetReading)

        var largestAccelAxis = 0
        for (i in accelReading.indices){
            if (abs(accelReading[i]) > abs(accelReading[largestAccelAxis])){
                largestAccelAxis = i
            }
        }

        // If the device is vertical, change the compass orientation to a different axis
        if (largestAccelAxis == 1) {
            SensorManager.remapCoordinateSystem(
                rotation,
                SensorManager.AXIS_X, SensorManager.AXIS_Z,
                rotation
            )
        }


        SensorManager.getOrientation(rotation, orientation)
    }

    /**
     * Detect if two angles are close to each other
     */
    private fun closeTo(current: Float, target: Float, within: Float): Boolean{
        val delta = deltaAngle(current, target)
        return abs(delta) <= within
    }

    /**
     * Get the difference between 2 angles
     */
    private fun deltaAngle(angle1: Float, angle2: Float): Float {
        var delta = angle2 - angle1
        delta += 180
        delta -= floor(delta / 360) * 360
        delta -= 180
        if (abs(abs(delta) - 180) <= Float.MIN_VALUE){
            delta = 180f
        }
        return delta
    }
}