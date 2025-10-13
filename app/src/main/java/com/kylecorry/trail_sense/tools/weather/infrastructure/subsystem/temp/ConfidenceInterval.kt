package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp

data class ConfidenceInterval<T>(val value: T, val upper: T, val lower: T)
