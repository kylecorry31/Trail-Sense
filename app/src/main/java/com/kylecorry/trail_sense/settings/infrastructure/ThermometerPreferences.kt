package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

class ThermometerPreferences(context: Context) : PreferenceRepo(context), IThermometerPreferences {
    // TODO: Populate these
    // TODO: Move calibration here
    override val source: ThermometerSource = ThermometerSource.Historic //by IntEnumPreference()
    override val smoothing: Float by FloatPreference(
        cache,
        context.getString(R.string.pref_temperature_smoothing),
        0.15f
    )
    override val temperatureOverride by FloatPreference(
        cache,
        context.getString(R.string.pref_temperature_override),
        0f
    )
}