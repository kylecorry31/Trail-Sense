package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideNameDisplay

class FieldGuideSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.field_guide_preferences, rootKey)

        list(R.string.pref_field_guide_name_display)?.apply {
            entries = arrayOf(
                getString(R.string.common_name),
                getString(R.string.scientific_name),
                getString(R.string.both_names)
            )
            entryValues = arrayOf(
                FieldGuideNameDisplay.Common.id.toString(),
                FieldGuideNameDisplay.Scientific.id.toString(),
                FieldGuideNameDisplay.Both.id.toString()
            )
        }
    }
}
