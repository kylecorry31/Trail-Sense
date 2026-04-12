package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R

class ARLayersBottomSheetPreferenceFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.augmented_reality_layer_sheet_preferences, rootKey)
    }
}