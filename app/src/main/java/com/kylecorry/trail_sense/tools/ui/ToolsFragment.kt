package com.kylecorry.trail_sense.tools.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.inclinometer.ui.InclinometerFragment
import com.kylecorry.trail_sense.shared.*

class ToolsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tools, rootKey)
        bindPreferences()
    }

    private fun bindPreferences() {
        fragmentOnClick(findPreference(getString(R.string.tool_inclinometer))) { InclinometerFragment() }
    }


    private fun fragmentOnClick(pref: Preference?, fragmentFactory: () -> Fragment) {
        pref?.setOnPreferenceClickListener {
            switchToFragment(fragmentFactory.invoke(), addToBackStack = true)
            false
        }
    }

}