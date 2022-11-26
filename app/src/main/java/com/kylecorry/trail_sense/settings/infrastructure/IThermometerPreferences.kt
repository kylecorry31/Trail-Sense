package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

interface IThermometerPreferences {
    val source: ThermometerSource
    val smoothing: Float
    val temperatureOverride: Float
}