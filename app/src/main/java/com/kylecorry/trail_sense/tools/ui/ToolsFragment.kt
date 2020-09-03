package com.kylecorry.trail_sense.tools.ui

import android.os.Bundle
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.inclinometer.ui.InclinometerFragment
import com.kylecorry.trail_sense.shared.*


class ToolsFragment : PreferenceFragmentCompat() {

    private lateinit var inclinometerPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tools, rootKey)

        bindPreferences()
    }

    private fun bindPreferences(){
        inclinometerPref = findPreference(getString(R.string.tool_inclinometer))!!


        inclinometerPref.setOnPreferenceClickListener {
            switchToFragment(InclinometerFragment(), addToBackStack = true)
            false
        }

    }


}