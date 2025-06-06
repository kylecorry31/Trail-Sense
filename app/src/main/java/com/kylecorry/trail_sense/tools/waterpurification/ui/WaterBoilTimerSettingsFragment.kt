package com.kylecorry.trail_sense.tools.waterpurification.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R

class WaterBoilTimerSettingsFragment : AndromedaPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.water_boil_timer_preferences, rootKey)
    }
}