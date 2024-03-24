package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.trail_sense.R

class ARLayersBottomSheetPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.augmented_reality_layer_sheet_preferences, rootKey)
    }
}