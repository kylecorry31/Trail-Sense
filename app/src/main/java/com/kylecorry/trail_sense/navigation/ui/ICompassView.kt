package com.kylecorry.trail_sense.navigation.ui

interface ICompassView {
    var visibility: Int
    var azimuth: Float
    var beacons: List<Float>
}