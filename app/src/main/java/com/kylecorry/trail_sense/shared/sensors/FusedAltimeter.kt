package com.kylecorry.trail_sense.shared.sensors

class FusedAltimeter(private val gps: IGPS, private val barometer: IBarometer) : AbstractSensor(),
    IAltimeter {

    override val altitude: Float
        get() = baseAltitude + altitudeChange

    private var altitudeChange = 0f
    private var lastBarometricAltitude: Float? = null

    private var baseAltitude = 0f

    private fun onGPSUpdate(): Boolean {
        baseAltitude = gps.altitude
        notifyListeners()
        return false
    }

    private fun onBarometerUpdate(): Boolean {
        val lastAltitude = lastBarometricAltitude
        if (lastAltitude != null) {
            altitudeChange += barometer.altitude - lastAltitude
        }

        lastBarometricAltitude = barometer.altitude
        notifyListeners()
        return true
    }


    override fun startImpl() {
        altitudeChange = 0f
        lastBarometricAltitude = null
        baseAltitude = gps.altitude
        gps.start(this::onGPSUpdate)
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        gps.stop(this::onGPSUpdate)
        barometer.stop(this::onBarometerUpdate)
    }
}