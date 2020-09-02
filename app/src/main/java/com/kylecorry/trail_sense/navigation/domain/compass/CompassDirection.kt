package com.kylecorry.trail_sense.navigation.domain.compass

enum class CompassDirection(val symbol: String, val azimuth: Float) {
    North("N", 0f),
    NorthEast("NE", 45f),
    East("E", 90f),
    SouthEast("SE", 135f),
    South("S", 180f),
    SouthWest("SW", 225f),
    West("W", 270f),
    NorthWest("NW", 315f)

}
