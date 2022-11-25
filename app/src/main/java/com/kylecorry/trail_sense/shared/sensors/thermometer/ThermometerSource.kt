package com.kylecorry.trail_sense.shared.sensors.thermometer

enum class ThermometerSource(val id: Int) {
    Historic(1),
    Sensor(2),
    Override(3)
}