package com.kylecorry.trail_sense.models

import java.time.Instant

data class PressureAltitudeReading(val time: Instant, val pressure: Float, val altitude: Float)