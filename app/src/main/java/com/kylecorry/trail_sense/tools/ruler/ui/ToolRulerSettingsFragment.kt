package com.kylecorry.trail_sense.tools.ruler.ui

import android.os.Bundle
import android.text.InputType
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R

class ToolRulerSettingsFragment : AndromedaPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_tool_ruler, rootKey)

        setIconColor(Resources.androidTextColorSecondary(requireContext()))

        editText(R.string.pref_ruler_calibration)
            ?.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }
    }

}