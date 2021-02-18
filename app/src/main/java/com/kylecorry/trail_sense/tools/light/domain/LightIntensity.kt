package com.kylecorry.trail_sense.tools.light.domain

enum class LightIntensity(val lux: Float) {
    NoMoon(0.001f),
    FullMoon(0.25f),
    Cloudy(100f),
    Sunrise(400f),
    Overcast(10000f),
    Shade(20000f),
    Sunlight(110000f),
}