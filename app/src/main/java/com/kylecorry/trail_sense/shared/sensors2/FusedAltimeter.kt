package com.kylecorry.trail_sense.shared.sensors2

class FusedAltimeter(private val gps: IGPS, private val barometer: IBarometer) : AbstractSensor(),
    IAltimeter {

    override val altitude: Float
        get() = baseAltitude + altitudeChange

    private var altitudeChange = 0f
    private var lastBarometricAltitude: Float? = null

    var baseAltitude = 0f

    private fun onGPSUpdate() {
        baseAltitude = gps.altitude
        gps.stop(this::onGPSUpdate)
        notifyListeners()
    }

    private fun onBarometerUpdate() {
        val lastAltitude = lastBarometricAltitude
        if (lastAltitude != null) {
            altitudeChange += barometer.altitude - lastAltitude
        }

        lastBarometricAltitude = barometer.altitude
        notifyListeners()
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