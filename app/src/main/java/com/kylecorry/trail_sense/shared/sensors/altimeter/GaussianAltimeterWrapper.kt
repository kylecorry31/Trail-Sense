package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.location.filters.GPSGaussianAltitudeFilter

class GaussianAltimeterWrapper(override val altimeter: IAltimeter, samples: Int = 4) :
    AbstractSensor(),
    AltimeterWrapper {

    private val filter = GPSGaussianAltitudeFilter(samples)

    // TODO: Add this to IAltimeter
    override val altitudeAccuracy: Float?
        get() = filter.accuracy

    override fun startImpl() {
        filter.reset()
        altimeter.start(this::onReading)
    }

    override fun stopImpl() {
        altimeter.stop(this::onReading)
    }

    override var altitude: Float = altimeter.altitude
        private set

    override val hasValidReading: Boolean
        get() = altimeter.hasValidReading && filter.hasValidReading

    override val quality: Quality
        get() = altimeter.quality

    private fun onReading(): Boolean {
        filter.update(
            altimeter.altitude,
            (altimeter as? IGPS)?.verticalAccuracy,
            altimeter.eventTimeElapsedNanos
        )
        altitude = filter.altitude
        if (hasValidReading) {
            notifyListeners()
        }
        return true
    }
}
