package com.kylecorry.trail_sense.shared.sensors.declination

import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.shared.sensors.AbstractSensor
import com.kylecorry.trail_sense.shared.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.IGPS

class AutoDeclinationProvider(private val gps: IGPS, private val altimeter: IAltimeter) :
    AbstractSensor(), IDeclinationProvider {
    override val declination: Float
        get() = _declination

    override val hasValidReading: Boolean
        get() = gotLocation && gotAltitude

    private var _declination = 0f
    private var started = false
    private var gotLocation = false
    private var gotAltitude = false

    private val declinationCalculator = DeclinationCalculator()

    init {
        if (gps.hasValidReading && altimeter.hasValidReading){
            gotLocation = true
            gotAltitude = true
            _declination = calculateDeclination()
        }
    }

    override fun startImpl() {
        started = true
        gps.start(this::onGPSUpdate)
        altimeter.start(this::onAltimeterUpdate)
    }

    override fun stopImpl() {
        started = false
        gps.stop(this::onGPSUpdate)
        altimeter.stop(this::onAltimeterUpdate)
    }

    private fun onGPSUpdate(): Boolean {
        gotLocation = true
        if (gotAltitude) {
            _declination = calculateDeclination()
            notifyListeners()
        }
        return started
    }

    private fun onAltimeterUpdate(): Boolean {
        gotAltitude = true
        if (gotLocation) {
            _declination = calculateDeclination()
            notifyListeners()
        }
        return started
    }

    private fun calculateDeclination(): Float {
        return declinationCalculator.calculate(gps.location, altimeter.altitude)
    }

}