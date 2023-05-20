package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource

interface ICompassPreferences {
    var compassSmoothing: Int
    var useTrueNorth: Boolean
    var source: CompassSource
}