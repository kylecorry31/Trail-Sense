package com.kylecorry.trail_sense.shared.sensors.compass

enum class CompassSource(val id: String) {
    RotationVector("rotation_vector"),
    GeomagneticRotationVector("geomagnetic_rotation_vector"),
    CustomMagnetometer("custom_magnetometer"),
    Orientation("orientation"),
    CustomRotationVector("custom_rotation_vector")
}