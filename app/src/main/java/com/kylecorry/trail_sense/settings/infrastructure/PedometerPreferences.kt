package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R

class PedometerPreferences(context: Context) : PreferenceRepo(context), IPedometerPreferences {
    override var isEnabled by BooleanPreference(
        cache,
        getString(R.string.pref_pedometer_enabled),
        false
    )

    override val resetDaily by BooleanPreference(
        cache,
        getString(R.string.pref_odometer_reset_daily),
        false
    )

    override var strideLength: Distance
        get() {
            val raw = cache.getFloat(getString(R.string.pref_stride_length)) ?: 0.7f
            return Distance.meters(raw)
        }
        set(value) {
            cache.putFloat(getString(R.string.pref_stride_length), value.meters().distance)
        }
}