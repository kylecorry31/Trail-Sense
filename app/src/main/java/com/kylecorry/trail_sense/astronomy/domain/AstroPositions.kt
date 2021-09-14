package com.kylecorry.trail_sense.astronomy.domain

data class AstroPositions(
    val moonAltitude: Float,
    val sunAltitude: Float,
    val moonAzimuth: Float,
    val sunAzimuth: Float
)