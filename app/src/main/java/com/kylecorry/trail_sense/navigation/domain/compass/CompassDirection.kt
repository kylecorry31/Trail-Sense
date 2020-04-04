package com.kylecorry.trail_sense.navigation.domain.compass

enum class CompassDirection(val symbol: String, val azimuth: Float) {
    NORTH("N", 0f),
    NORTHEAST("NE", 45f),
    EAST("E", 90f),
    SOUTHEAST("SE", 135f),
    SOUTH("S", 180f),
    SOUTHWEST("SE", 225f),
    WEST("W", 270f),
    NORTHWEST("NW", 315f)

}