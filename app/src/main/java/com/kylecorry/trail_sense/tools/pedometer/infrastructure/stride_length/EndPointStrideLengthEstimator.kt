package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

class EndPointStrideLengthEstimator(private val gps: IGPS, private val pedometer: IPedometer) :
    AbstractSensor(), IStrideLengthEstimator {

    override var strideLength: Distance? = null
        private set

    override val hasValidReading: Boolean
        get() = strideLength != null

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
        updateStrideLength()
        return true
    }

    private fun onPedometer(): Boolean {
        if (startSteps == null) {
            startSteps = pedometer.steps.toLong()
        }
        updateStrideLength()
        return true
    }

    override fun reset() {
        startLocation = null
        startSteps = null
        updateStrideLength()
    }

    private fun updateStrideLength() {
        val startLocation = startLocation
        val startSteps = startSteps

        if (startLocation == null || startSteps == null) {
            strideLength = null
            return
        }

        val distance = gps.location.distanceTo(startLocation)
        val steps = pedometer.steps - startSteps

        strideLength = if (steps == 0L) {
            Distance.meters(0f)
        } else {
            Distance.meters(distance / steps)
        }
        notifyListeners()
    }
}