package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.barometer.IBarometer

class FusedAltimeter(private val gps: IGPS, private val barometer: IBarometer) : AbstractSensor(),
    IAltimeter {

    override val altitude: Float
        get() = baseAltitude + altitudeChange

    override val hasValidReading: Boolean
        get() = gotGps

    private var altitudeChange = 0f
    private var lastBarometricAltitude: Float? = null

    private var baseAltitude = 0f
    private var started = false

    private var gotGps = false

    init {
        if (gps.hasValidReading){
            gotGps = true
            baseAltitude = gps.altitude
        }
    }

    private fun onGPSUpdate(): Boolean {
        baseAltitude = gps.altitude
        gotGps = true
        notifyListeners()
        return false
    }

    private fun onBarometerUpdate(): Boolean {
        val lastAltitude = lastBarometricAltitude
        if (lastAltitude != null) {
            altitudeChange += barometer.altitude - lastAltitude
        }

        lastBarometricAltitude = barometer.altitude

        if (gotGps) {
            notifyListeners()
        }
        return true
    }


    override fun startImpl() {
        altitudeChange = 0f
        started = true
        lastBarometricAltitude = null
        baseAltitude = gps.altitude
        gps.start(this::onGPSUpdate)
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        started = false
        gps.stop(this::onGPSUpdate)
        barometer.stop(this::onBarometerUpdate)
    }
}