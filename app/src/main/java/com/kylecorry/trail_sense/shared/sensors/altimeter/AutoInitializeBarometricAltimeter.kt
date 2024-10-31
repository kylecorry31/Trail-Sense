package com.kylecorry.trail_sense.shared.sensors.altimeter

import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.sense.altitude.BarometricAltimeter
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure

class AutoInitializeBarometricAltimeter(
    private val barometer: IBarometer,
    private val cachedAltimeter: CachedAltimeter
) : AbstractSensor(),
    IAltimeter {

    private var isRunning = false
    private var altimeter: IAltimeter? = null

    override fun startImpl() {
        isRunning = true
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        isRunning = false
        barometer.stop(this::onBarometerUpdate)
        altimeter?.stop(this::onAltimeterUpdate)
        altimeter = null
    }

    override val hasValidReading: Boolean
        get() = altimeter?.hasValidReading == true

    override val altitude: Float
        get() = altimeter?.altitude ?: cachedAltimeter.altitude

    private fun onBarometerUpdate(): Boolean {
        val pressure = barometer.pressure
        if (pressure == 0f) {
            return true
        }

        // Initialize the altimeter
        var lastAltitude = cachedAltimeter.altitude

        val seaLevelPressure = if (lastAltitude != 0f) {
            Meteorology.getSeaLevelPressure(
                Pressure.hpa(pressure),
                Distance.meters(lastAltitude),
            )
        } else {
            Pressure.hpa(SensorManager.PRESSURE_STANDARD_ATMOSPHERE)
        }

        if (isRunning) {
            altimeter?.stop(this::onAltimeterUpdate)
            altimeter = BarometricAltimeter(barometer, seaLevelPressure)
            altimeter?.start(this::onAltimeterUpdate)
        }

        return false
    }

    private fun onAltimeterUpdate(): Boolean {
        notifyListeners()
        return true
    }
}