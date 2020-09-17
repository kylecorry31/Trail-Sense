package com.kylecorry.trail_sense.shared.sensors

import android.hardware.SensorManager
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer

class BarometricAltimeter(private val barometer: IBarometer, private val seaLevelPressureFactory: () -> Float) : AbstractSensor(),
    IAltimeter {

    override val altitude: Float
        get() = SensorManager.getAltitude(seaLevelPressureFactory.invoke(), barometer.pressure)

    override val hasValidReading: Boolean
        get() = barometer.hasValidReading

    private fun onBarometerUpdate(): Boolean {
        notifyListeners()
        return true
    }

    override fun startImpl() {
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        barometer.stop(this::onBarometerUpdate)
    }
}