package com.kylecorry.trail_sense.tools.ballistics.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class BallisticsPreferences(context: Context) : PreferenceRepo(context) {

    val isBallisticsCalculatorEnabled by BooleanPreference(
        cache,
        getString(R.string.pref_ballistics_calculator_enabled),
        false
    )


}