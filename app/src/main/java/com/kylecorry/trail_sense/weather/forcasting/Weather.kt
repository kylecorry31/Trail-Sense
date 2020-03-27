package com.kylecorry.trail_sense.weather.forcasting

enum class Weather(val description: String) {

    // Weather direction
    ImprovingFast("Quickly improving"),
    WorseningFast("Quickly worsening"),
    ImprovingSlow("Improving"),
    WorseningSlow("Worsening"),
    NoChange("Not changing"),
    Unknown("Unknown"),

}