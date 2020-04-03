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
class OrientationCompass (ctx: Context) : ICompass, ISensor, SensorEventListener, Observable() {

    private var sensorManager: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var totalAngle = 0f

    private var started = false

    private val azimuthKf = KalmanFilter(0.5, 0.0001)

    private var _azimuth = 0f

    /**
     * The direction in degrees that the compass is facing (called azimuth)
     */
    override val azimuth: Bearing
        get() {
            return Bearing(_azimuth + declination)
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
        sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.also { orientation ->
            sensorManager.registerListener(
                this,
                orientation,
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
        if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            _azimuth = event.values[0]
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