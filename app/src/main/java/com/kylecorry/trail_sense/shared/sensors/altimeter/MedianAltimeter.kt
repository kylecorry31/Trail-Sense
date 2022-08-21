package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.sol.math.filters.MedianFilter

class MedianAltimeter(val altimeter: IAltimeter, private val samples: Int = 5) :
    AbstractSensor(), IAltimeter {

    private val filter = MedianFilter(samples)
    private var count = 0

    override fun startImpl() {
        count = 0
        altimeter.start(this::onReading)
    }

    override fun stopImpl() {
        altimeter.stop(this::onReading)
    }

    override var altitude: Float = altimeter.altitude
        private set


    override val hasValidReading: Boolean
        get() = altimeter.hasValidReading && count >= samples

    private fun onReading(): Boolean {
        count++
        altitude = filter.filter(altimeter.altitude)
        if (hasValidReading){
            notifyListeners()
        }
        return true
    }
}