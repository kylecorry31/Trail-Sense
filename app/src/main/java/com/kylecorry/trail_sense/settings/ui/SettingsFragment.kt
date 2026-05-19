package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort

class SettingsFragment : AndromedaPreferenceFragment() {

    private val navigationMap = mapOf(
        R.string.pref_unit_settings to R.id.action_settings_to_unit_settings,
        R.string.pref_privacy_settings to R.id.action_settings_to_privacy_settings,
        R.string.pref_theme_settings to R.id.action_settings_to_theme_settings,
        R.string.pref_experimental_settings to R.id.action_settings_to_experimental_settings,
        R.string.pref_plugins_settings to R.id.action_settings_to_plugins_settings,
        R.string.pref_error_settings to R.id.action_settings_to_error_settings,
        R.string.pref_sensor_settings to R.id.action_settings_to_sensor_settings,
        R.string.pref_backup_settings to R.id.action_settings_to_backup_settings,
        R.string.pref_tool_settings_header_key to R.id.toolsSettingsFragment,
        // About
        R.string.pref_open_source_licenses to R.id.action_action_settings_to_licenseFragment,
        R.string.pref_diagnostics to R.id.action_settings_to_diagnostics
    )

    private val plugins by lazy { getAppService<PluginSubsystem>() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        preference(R.string.pref_plugins_settings)?.isVisible = plugins.arePluginsEnabled()

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        onClick(preference(R.string.pref_github)) {
            val i = Intents.url(it.summary.toString())
            startActivity(i)
        }

        onClick(preference(R.string.pref_privacy_policy)) {
            val i = Intents.url(it.summary.toString())
            startActivity(i)
        }

        onClick(preference(R.string.pref_email)) {
            val intent = Intents.email(it.summary.toString(), getString(R.string.app_name))
            startActivity(Intent.createChooser(intent, it.title.toString()))
        }

        val version = Package.getVersionName(requireContext())
        preference(R.string.pref_app_version)?.summary = version
        setIconColor(preferenceScreen, Resources.androidTextColorSecondary(requireContext()))

        // Populate tool settings
        val toolCategoryPreference =
            findPreference<PreferenceCategory>(getString(R.string.pref_tool_category_holder_key))
        val tools = Tools.getTools(requireContext())
        val sortedTools = AlphabeticalToolSort().sort(tools)
        val primaryColor = Resources.androidTextColorPrimary(requireContext())
        for (tool in sortedTools.first().tools) {
            if (tool.settingsNavAction == null) {
                continue
            }
            val preference = Preference(requireContext())
            preference.title = tool.name
            preference.setIcon(tool.icon)
            preference.icon?.let {
                Colors.setImageColor(it, primaryColor)
            }
            preference.setOnPreferenceClickListener {
                findNavController().navigateWithAnimation(tool.settingsNavAction)
                true
            }
            toolCategoryPreference?.addPreference(preference)
        }
    }

}
