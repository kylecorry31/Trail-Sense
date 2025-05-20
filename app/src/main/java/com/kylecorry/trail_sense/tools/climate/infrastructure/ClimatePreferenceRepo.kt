package com.kylecorry.trail_sense.tools.climate.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class ClimatePreferenceRepo(context: Context) : PreferenceRepo(context) {

    val isInsectActivityEnabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_climate_insect_activity_enabled),
        false
    )


}