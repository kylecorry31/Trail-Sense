package com.kylecorry.trail_sense.weather.domain

import org.threeten.bp.Instant

data class PressureAltitudeReading(val time: Instant, val pressure: Float, val altitude: Float)