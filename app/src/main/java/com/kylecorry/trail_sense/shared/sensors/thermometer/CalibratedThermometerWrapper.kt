package com.kylecorry.trail_sense.shared.sensors.thermometer

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.sol.units.Temperature

class CalibratedThermometerWrapper(
    private val thermometer: IThermometer,
    private val calibrator: ITemperatureCalibrator
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
        return calibrator.calibrate(Temperature.celsius(temperature)).temperature
    }
}