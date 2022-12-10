package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

class ThermometerPreferences(context: Context) : PreferenceRepo(context), IThermometerPreferences {
    // TODO: Move calibration here
    override val source by StringEnumPreference(
        cache, getString(R.string.pref_thermometer_source),
        mapOf(
            "historic" to ThermometerSource.Historic,
            "sensor" to ThermometerSource.Sensor
        ),
        ThermometerSource.Historic
    )

    override var smoothing: Float
        get() {
            return (cache.getInt(context.getString(R.string.pref_temperature_smoothing))
                ?: 0) / 1000f
        }
        set(value) {
            val scaled = (value * 1000).coerceIn(0f, 1000f)
            cache.putInt(
                context.getString(R.string.pref_temperature_smoothing),
                scaled.toInt()
            )
        }
}