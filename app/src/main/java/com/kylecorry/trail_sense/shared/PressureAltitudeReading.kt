package com.kylecorry.trail_sense.shared

import java.time.Instant

data class PressureAltitudeReading(val time: Instant, val pressure: Float, val altitude: Float)