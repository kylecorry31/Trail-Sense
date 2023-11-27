package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R

class AltimeterPreferences(context: Context): PreferenceRepo(context) {

    val useContinuousCalibration by BooleanPreference(
        cache,
        context.getString(R.string.pref_altimeter_continuous_calibration),
        false
    )

}