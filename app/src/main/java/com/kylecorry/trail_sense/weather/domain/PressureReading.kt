package com.kylecorry.trail_sense.weather.domain

import org.threeten.bp.Instant

data class PressureReading(val time: Instant, val value: Float)