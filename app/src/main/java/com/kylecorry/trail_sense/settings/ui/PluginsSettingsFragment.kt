package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.plugins.AvailablePlugin
import com.kylecorry.trail_sense.plugins.plugins.PluginSubsystem
import kotlinx.coroutines.launch

class PluginsSettingsFragment : AndromedaPreferenceFragment() {

    private val pluginSubsystem by lazy { PluginSubsystem.getInstance(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.empty_preferences, rootKey)
        preferenceScreen.setShouldUseGeneratedIds(true)
        refreshPlugins()
    }

    private fun refreshPlugins() {
        lifecycleScope.launch {
            val connected = pluginSubsystem.getConnectedPlugins()
            val unconnected = pluginSubsystem.getUnconnectedPlugins()

            preferenceScreen.removeAll()
            addCategory(R.string.connected_plugins, connected.sorted(), ::confirmDisconnect)
            addCategory(R.string.available_plugins, unconnected.sorted(), ::confirmConnect)
        }
    }

    private fun addCategory(
        title: Int,
        plugins: List<AvailablePlugin>,
        onClick: (AvailablePlugin) -> Unit
    ) {
        val category = PreferenceCategory(requireContext())
        category.title = getString(title)
        category.isIconSpaceReserved = false
        category.isSingleLineTitle = false
        preferenceScreen.addPreference(category)

        if (plugins.isEmpty()) {
            val preference = Preference(requireContext())
            preference.title = getString(R.string.none)
            preference.isEnabled = false
            preference.isIconSpaceReserved = false
            preference.isSingleLineTitle = false
            category.addPreference(preference)
            return
        }

        for (plugin in plugins) {
            val preference = Preference(requireContext())
            preference.title = plugin.name
            preference.summary = plugin.packageId
            preference.isIconSpaceReserved = false
            preference.isSingleLineTitle = false
            preference.setOnPreferenceClickListener {
                onClick(plugin)
                true
            }
            category.addPreference(preference)
        }
    }

    private fun confirmConnect(plugin: AvailablePlugin) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.plugin_connect_title),
            getString(R.string.plugin_connect_message, plugin.name)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    pluginSubsystem.connect(plugin)
                    refreshPlugins()
                }
            }
        }
    }

    private fun confirmDisconnect(plugin: AvailablePlugin) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.plugin_disconnect_title),
            getString(R.string.plugin_disconnect_message, plugin.name)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    pluginSubsystem.disconnect(plugin)
                    refreshPlugins()
                }
            }
        }
    }

    private fun List<AvailablePlugin>.sorted(): List<AvailablePlugin> {
        return sortedWith(
            compareBy<AvailablePlugin, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.packageId }
        )
    }
}
