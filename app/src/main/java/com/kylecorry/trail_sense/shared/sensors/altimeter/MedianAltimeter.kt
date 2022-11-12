package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.math.filters.MedianFilter

class MedianAltimeter(override val altimeter: IAltimeter, private val samples: Int = 4) :
    AbstractSensor(), FilteredAltimeter {

    private val filter = MedianFilter(samples)
    private var count = 0

    override fun startImpl() {
        count = 0
        altimeter.start(this::onReading)
    }

    override fun stopImpl() {
        altimeter.stop(this::onReading)
    }

    override val altitudeAccuracy: Float
        get() = if (altimeter is IGPS) altimeter.verticalAccuracy ?: 10f else 10f

    override var altitude: Float = altimeter.altitude
        private set


    override val hasValidReading: Boolean
        get() = altimeter.hasValidReading && count >= samples

    private fun onReading(): Boolean {
        count++
        altitude = filter.filter(altimeter.altitude)
        if (hasValidReading) {
            notifyListeners()
        }
        return true
    }
}