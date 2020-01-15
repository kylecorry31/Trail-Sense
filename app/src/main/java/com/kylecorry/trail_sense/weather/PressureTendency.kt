package com.kylecorry.trail_sense.weather

enum class PressureTendency(val readableName: String) {
    FALLING_SLOW("Weather may worsen"),
    RISING_SLOW("Weather may improve"),
    FALLING_FAST("Weather will worsen soon"),
    RISING_FAST("Weather will improve soon "),
    NO_CHANGE("Weather not changing")
}