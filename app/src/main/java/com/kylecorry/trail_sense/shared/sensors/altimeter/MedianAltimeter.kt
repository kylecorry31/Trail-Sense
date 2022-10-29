package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.filters.MedianFilter
import com.kylecorry.sol.math.sumOfFloat
import kotlin.math.sqrt

class MedianAltimeter(val altimeter: IAltimeter, private val samples: Int = 3) :
    AbstractSensor(), IAltimeter {

    private val filter = MedianFilter(samples)
    private var count = 0
    private val buffer = mutableListOf<Pair<Float, Float>>()

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
        buffer.add(
            altimeter.altitude to (if (altimeter is IGPS) altimeter.verticalAccuracy
                ?: 10f else 10f)
        )
        if (buffer.size > samples) {
            buffer.removeFirst()
        }
        val calculated = calculate()
        println(calculated)
        altitude = filter.filter(altimeter.altitude)
        println(altitude)
        if (hasValidReading) {
            notifyListeners()
        }
        return true
    }


    private fun calculate(): Pair<Float, Float> {
        val sum = buffer.sumOfFloat { 1 / it.second }
        val weights = buffer.map { (1 / it.second) / sum }
        var mean = 0f
        var variance = 0f
        for (i in buffer.indices) {
            mean += weights[i] * buffer[i].first
            variance += SolMath.square(weights[i] * buffer[i].second)
        }
        return mean to sqrt(variance)
    }
}