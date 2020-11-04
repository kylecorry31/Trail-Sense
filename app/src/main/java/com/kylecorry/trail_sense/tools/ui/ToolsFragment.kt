package com.kylecorry.trail_sense.tools.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*

class ToolsFragment : PreferenceFragmentCompat() {

    private lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tools, rootKey)
        bindPreferences()
    }

    private fun bindPreferences() {
        navigateOnClick(
            findPreference(getString(R.string.tool_user_guide)),
            R.id.action_action_experimental_tools_to_guideListFragment
        )
        navigateOnClick(
            findPreference(getString(R.string.tool_bubble_level)),
            R.id.action_action_experimental_tools_to_levelFragment
        )
        navigateOnClick(
            findPreference(getString(R.string.tool_inclinometer)),
            R.id.action_toolsFragment_to_inclinometerFragment
        )
        navigateOnClick(
            findPreference(getString(R.string.tool_inventory)),
            R.id.action_action_experimental_tools_to_action_inventory
        )

        val maps = findPreference<Preference>(getString(R.string.tool_trail_sense_maps))
        maps?.isVisible = TrailSenseMaps.isInstalled(requireContext())
        onClick(maps) { TrailSenseMaps.open(requireContext()) }
    }


    private fun onClick(pref: Preference?, action: () -> Unit) {
        pref?.setOnPreferenceClickListener {
            action.invoke()
            true
        }
    }

    private fun navigateOnClick(pref: Preference?, @IdRes action: Int, bundle: Bundle? = null) {
        pref?.setOnPreferenceClickListener {
            navController.navigate(action, bundle)
            false
        }
    }

}