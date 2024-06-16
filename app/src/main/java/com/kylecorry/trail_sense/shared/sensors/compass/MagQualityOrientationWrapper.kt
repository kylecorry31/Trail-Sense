package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.Quaternion
import kotlin.math.min

class MagQualityOrientationWrapper(
    private val orientationSensor: IOrientationSensor,
    private val magnetometer: ISensor
) : AbstractSensor(), IOrientationSensor {

    override val hasValidReading: Boolean
        get() = orientationSensor.hasValidReading

    override val headingAccuracy: Float?
        get() = orientationSensor.headingAccuracy

    override val orientation: Quaternion
        get() = orientationSensor.orientation

    override val quality: Quality
        get() = Quality.entries[min(
            magnetometer.quality.ordinal,
            orientationSensor.quality.ordinal
        )]

    override val rawOrientation: FloatArray
        get() = orientationSensor.rawOrientation

    override fun startImpl() {
        orientationSensor.start(this::onReading)
        magnetometer.start(this::onReading)
    }

    override fun stopImpl() {
        orientationSensor.stop(this::onReading)
        magnetometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }
}