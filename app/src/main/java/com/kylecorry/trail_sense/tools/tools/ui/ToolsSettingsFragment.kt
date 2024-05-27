package com.kylecorry.trail_sense.tools.tools.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolsSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tool_preferences, rootKey)

        onClick(findPreference(getString(R.string.pref_tool_quick_action_header_key))) {
            val potentialActions = Tools.getQuickActions(requireContext())
                .filterNot { it.id == Tools.QUICK_ACTION_NONE }

            val selected = prefs.toolQuickActions

            val selectedIndices = potentialActions.mapIndexedNotNull { index, action ->
                if (selected.contains(action.id)) index else null
            }

            Pickers.items(
                requireContext(),
                getString(R.string.quick_actions),
                potentialActions.map { it.name },
                selectedIndices
            ) {
                if (it != null) {
                    prefs.toolQuickActions = it.map { potentialActions[it].id }
                }
            }
        }
    }
}