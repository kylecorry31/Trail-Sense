package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.google.android.material.color.DynamicColors
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme

class ThemeSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.theme_preferences, rootKey)

        reloadThemeOnChange(list(R.string.pref_theme))

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

        // Set Compact Mode
        val compactMode = switch(R.string.pref_use_compact_mode)
        compactMode?.setOnPreferenceChangeListener { _, checked ->
            requireMainActivity().changeBottomNavLabelsVisibility(checked as Boolean)
            true
        }

        // Widget default theme
        val widgetTheme = list(R.string.pref_default_widget_theme)!!
        val items = listOf(
            WidgetTheme.System to getString(R.string.theme_system),
            WidgetTheme.TransparentBlack to getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_black_text)
            ),
            WidgetTheme.TransparentWhite to getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_white_text)
            )
        )
        widgetTheme.entries = items.map { it.second }.toTypedArray()
        widgetTheme.entryValues = items.map { it.first.id.toString() }.toTypedArray()
        // Save the default value
        if (AppServiceRegistry.get<PreferencesSubsystem>().preferences.getString(widgetTheme.key) == null) {
            widgetTheme.value = WidgetTheme.System.id.toString()
        }

        onChange(widgetTheme) {
            Tools.triggerWidgetUpdate(requireContext(), null)
        }
    }

    private fun reloadThemeOnChange(pref: androidx.preference.Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivity().reloadTheme()
            true
        }
    }

}
