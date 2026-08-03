package com.kylecorry.trail_sense.tools.pedometer.infrastructure.step_length

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

class EndPointStepLengthEstimator(private val gps: IGPS, private val pedometer: IPedometer) :
    AbstractSensor(), IStepLengthEstimator {

    override var stepLength: Distance? = null
        private set

    override val hasValidReading: Boolean
        get() = stepLength != null

    private var startLocation: Coordinate? = null
    private var startSteps: Long? = null

    override fun startImpl() {
        gps.start(this::onGPS)
        pedometer.start(this::onPedometer)
    }

    override fun stopImpl() {
        gps.stop(this::onGPS)
        pedometer.stop(this::onPedometer)
    }

    private fun onGPS(): Boolean {
        if (startLocation == null) {
            startLocation = gps.location
        }
        updateStepLength()
        return true
    }

    private fun onPedometer(): Boolean {
        if (startSteps == null) {
            startSteps = pedometer.steps.toLong()
        }
        updateStepLength()
        return true
    }

    override fun reset() {
        startLocation = null
        startSteps = null
        updateStepLength()
    }

    private fun updateStepLength() {
        val startLocation = startLocation
        val startSteps = startSteps

        if (startLocation == null || startSteps == null) {
            stepLength = null
            return
        }

        val distance = gps.location.distanceTo(startLocation)
        val steps = pedometer.steps - startSteps

        stepLength = if (steps == 0L) {
            Distance.meters(0f)
        } else {
            Distance.meters(distance / steps)
        }
        notifyListeners()
    }
}
