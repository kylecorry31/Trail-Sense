package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.math.Range
import java.time.Instant

data class StepSession(val steps: Int, val time: Range<Instant>)
