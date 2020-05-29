package com.kylecorry.trail_sense.weather.domain

import java.time.Instant

data class AltitudeReading(val time: Instant, val value: Float)