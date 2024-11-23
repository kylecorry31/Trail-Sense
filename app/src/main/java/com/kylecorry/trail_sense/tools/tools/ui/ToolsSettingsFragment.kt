package com.kylecorry.trail_sense.tools.tools.ui

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort

class ToolsSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private var bottomNavSelection: List<Long> = listOf()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tool_preferences, rootKey)
        bindQuickActionPreference()
        bindWidgetPreference()
        bindBottomNavPreferences()
    }

    private fun bindBottomNavPreferences() {
        val toolsPreferenceCategory =
            findPreference<PreferenceCategory>(getString(R.string.pref_tool_category_holder_key))

        val tools = AlphabeticalToolSort().sort(Tools.getTools(requireContext())).first().tools
        bottomNavSelection = prefs.bottomNavigationTools

        val names = listOf(getString(R.string.none)) + tools.map { it.name }
        val ids = listOf(0L) + tools.map { it.id }

        // For each available slot, create a preference
        for (i in bottomNavSelection.indices) {
            val preference = Preference(requireContext())
            preference.title = getString(R.string.bottom_navigation_slot, i + 1)
            preference.summary =
                tools.firstOrNull { it.id == bottomNavSelection.getOrNull(i) }?.name ?: getString(
                    R.string.none
                )
            preference.setOnPreferenceClickListener {
                Pickers.item(
                    requireContext(),
                    preference.title as String,
                    names,
                    ids.indexOfFirst { it == bottomNavSelection.getOrNull(i) }
                ) {
                    if (it != null) {
                        val selected = ids[it]
                        bottomNavSelection =
                            prefs.bottomNavigationTools.mapIndexed { index, id ->
                                if (index == i) selected.toLong() else id
                            }
                        prefs.bottomNavigationTools = bottomNavSelection
                        preference.summary = names[it]
                        requireMainActivity().updateBottomNavigation()
                    }
                }
                true
            }
            preference.isIconSpaceReserved = false
            preference.isSingleLineTitle = true
            toolsPreferenceCategory?.addPreference(preference)
        }

        // Add a fixed slot for the tools
        val toolSlotPreference = Preference(requireContext())
        toolSlotPreference.title =
            getString(R.string.bottom_navigation_slot, bottomNavSelection.size + 1)
        toolSlotPreference.summary = getString(R.string.tools)
        toolSlotPreference.isEnabled = false
        toolSlotPreference.isIconSpaceReserved = false
        toolSlotPreference.isSingleLineTitle = true
        toolsPreferenceCategory?.addPreference(toolSlotPreference)
    }

    private fun bindQuickActionPreference() {
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

    private fun bindWidgetPreference() {
        onClick(findPreference(getString(R.string.pref_tool_widgets_header_key))) {
            val tools = Tools.getTools(requireContext())
            val potentialWidgets = tools
                .flatMap { it.widgets }
                .filter { it.isEnabled(requireContext()) && it.canPlaceInApp }
                .distinctBy { it.id }
                .sortedBy { it.name }

            val selected = prefs.toolWidgets

            val selectedIndices = potentialWidgets.mapIndexedNotNull { index, widget ->
                if (selected.contains(widget.id)) index else null
            }

            Pickers.items(
                requireContext(),
                getString(R.string.widget_pref_title),
                potentialWidgets.map { it.name },
                selectedIndices
            ) {
                if (it != null) {
                    prefs.toolWidgets = it.map { potentialWidgets[it].id }
                }
            }
        }
    }
}