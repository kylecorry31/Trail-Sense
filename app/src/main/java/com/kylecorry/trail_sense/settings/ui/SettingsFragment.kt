package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.color.DynamicColors
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.backup.BackupCommand
import com.kylecorry.trail_sense.settings.backup.ChangeAutomaticBackupDirectoryCommand
import com.kylecorry.trail_sense.settings.backup.RestoreCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort
import kotlinx.coroutines.launch

class SettingsFragment : AndromedaPreferenceFragment() {

    private val navigationMap = mapOf(
        R.string.pref_unit_settings to R.id.action_settings_to_unit_settings,
        R.string.pref_privacy_settings to R.id.action_settings_to_privacy_settings,
        R.string.pref_experimental_settings to R.id.action_settings_to_experimental_settings,
        R.string.pref_error_settings to R.id.action_settings_to_error_settings,
        R.string.pref_sensor_settings to R.id.action_settings_to_sensor_settings,
        R.string.pref_tool_settings_header_key to R.id.toolsSettingsFragment,
        // About
        R.string.pref_open_source_licenses to R.id.action_action_settings_to_licenseFragment,
        R.string.pref_diagnostics to R.id.action_settings_to_diagnostics
    )

    private val uriPicker by lazy { IntentUriPicker(this, requireContext()) }
    private val backupCommand by lazy { BackupCommand(requireContext(), uriPicker) }
    private val restoreCommand by lazy { RestoreCommand(requireContext(), uriPicker) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        reloadThemeOnChange(list(R.string.pref_theme))

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

        val dynamicColorsSwitch = switch(R.string.pref_use_dynamic_colors)
        val dynamicCompassColorsSwitch = switch(R.string.pref_use_dynamic_colors_on_compass)
        dynamicColorsSwitch?.isVisible = DynamicColors.isDynamicColorAvailable()
        dynamicCompassColorsSwitch?.isVisible = DynamicColors.isDynamicColorAvailable()
        dynamicCompassColorsSwitch?.isEnabled = prefs.useDynamicColors
        dynamicColorsSwitch?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivity().reloadTheme()
            dynamicCompassColorsSwitch?.isEnabled = prefs.useDynamicColors
            true
        }

        //Set Compact Mode
        val compactMode = switch(R.string.pref_use_compact_mode)
        compactMode?.setOnPreferenceChangeListener { _, checked ->
            requireMainActivity().changeBottomNavLabelsVisibility(checked as Boolean)
            true
        }

        val version = Package.getVersionName(requireContext())
        preference(R.string.pref_app_version)?.summary = version
        setIconColor(preferenceScreen, Resources.androidTextColorSecondary(requireContext()))

        onClick(findPreference("backup_restore")) {
            Pickers.item(
                requireContext(),
                getString(R.string.backup_restore),
                listOf(
                    getString(R.string.backup),
                    getString(R.string.restore)
                )
            ) {
                when (it) {
                    0 -> backup()
                    1 -> restore()
                }
            }
        }

        val autoBackupSwitch = switch(R.string.pref_auto_backup_enabled)
        autoBackupSwitch?.summary = if (prefs.backup.isAutoBackupEnabled) {
            prefs.backup.userFriendlyAutoBackupPathName
        } else {
            null
        }

        onClick(autoBackupSwitch) {
            if (prefs.backup.isAutoBackupEnabled) {
                ChangeAutomaticBackupDirectoryCommand(requireContext(), this) {
                    if (!it) {
                        prefs.backup.isAutoBackupEnabled = false
                        autoBackupSwitch?.summary = null
                    } else {
                        autoBackupSwitch?.summary = prefs.backup.userFriendlyAutoBackupPathName
                    }
                }.execute()
            } else {
                autoBackupSwitch?.summary = null
            }
        }

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

    private fun backup() {
        lifecycleScope.launch {
            backupCommand.execute()
        }
    }

    private fun restore() {
        lifecycleScope.launch {
            restoreCommand.execute()
        }
    }

    private fun reloadThemeOnChange(pref: Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivity().reloadTheme()
            true
        }
    }

}