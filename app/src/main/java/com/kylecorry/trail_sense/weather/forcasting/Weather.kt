package com.kylecorry.trail_sense.weather.forcasting

enum class Weather(val description: String) {

    // Weather direction
    ImprovingFast("Rapidly improving"),
    WorseningFast("Rapidly worsening"),
    ImprovingSlow("Improving"),
    WorseningSlow("Worsening"),
    NoChange("Not changing"),
    Storm("Storm incoming"),
    Unknown("Unknown"),

}