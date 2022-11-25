package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

class ThermometerPreferences(context: Context) : PreferenceRepo(context), IThermometerPreferences {
    // TODO: Populate these
    // TODO: Move calibration here
    override val source: ThermometerSource = ThermometerSource.Historic //by IntEnumPreference()
    override val smoothing: Float
        get() = if (source == ThermometerSource.Sensor) {
            0.2f
        } else {
            0f
        }
}