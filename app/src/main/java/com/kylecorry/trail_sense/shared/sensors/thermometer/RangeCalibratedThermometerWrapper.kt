package com.kylecorry.trail_sense.shared.sensors.thermometer

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.sol.math.SolMath

class RangeCalibratedThermometerWrapper(
    private val thermometer: IThermometer,
    private val sensorMin: Float,
    private val sensorMax: Float,
    private val calibratedMin: Float,
    private val calibratedMax: Float
) : IThermometer, AbstractSensor() {
    override val hasValidReading: Boolean
        get() = thermometer.hasValidReading

    override val temperature: Float
        get() = calibrate(thermometer.temperature)

    override fun startImpl() {
        thermometer.start(this::onReading)
    }

    override fun stopImpl() {
        thermometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }

    private fun calibrate(temperature: Float): Float {
        return SolMath.map(temperature, sensorMin, sensorMax, calibratedMin, calibratedMax)
    }
}