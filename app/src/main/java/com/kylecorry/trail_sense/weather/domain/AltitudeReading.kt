package com.kylecorry.trail_sense.weather.domain

import org.threeten.bp.Instant

data class AltitudeReading(val time: Instant, val value: Float)