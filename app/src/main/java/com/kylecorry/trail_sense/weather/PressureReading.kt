package com.kylecorry.trail_sense.weather

import java.time.Instant

data class PressureReading(val time: Instant, val pressure: Float, val altitude: Double)