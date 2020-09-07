package com.kylecorry.trail_sense.weather.domain

import java.time.Instant

data class PressureAltitudeReading(val time: Instant, val pressure: Float, val altitude: Float, val temperature: Float)