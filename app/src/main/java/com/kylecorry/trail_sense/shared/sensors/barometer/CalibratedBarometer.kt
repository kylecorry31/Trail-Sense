package com.kylecorry.trail_sense.shared.sensors.barometer

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.barometer.IBarometer

class CalibratedBarometer(private val barometer: IBarometer, private val offset: Float) :
    IBarometer, AbstractSensor() {

    override val hasValidReading: Boolean
        get() = barometer.hasValidReading

    override val pressure: Float
        get() = barometer.pressure + offset

    override val quality: Quality
        get() = barometer.quality

    override fun startImpl() {
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        barometer.stop(this::onBarometerUpdate)
    }

    private fun onBarometerUpdate(): Boolean {
        notifyListeners()
        return true
    }

}