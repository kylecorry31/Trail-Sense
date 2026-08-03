package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import java.time.Duration

class StepLengthPaceCalculator(private val stepLength: Distance) : IPaceCalculator {
    override fun distance(steps: Long): Distance {
        return Distance.from(steps * stepLength.value, stepLength.units)
    }

    override fun speed(steps: Long, time: Duration): Speed {
        val d = distance(steps)
        val seconds = time.seconds
        if (seconds <= 0) {
            return ZERO_SPEED
        }

        return Speed.from(d.value / seconds, d.units, TimeUnits.Seconds)
    }
}
