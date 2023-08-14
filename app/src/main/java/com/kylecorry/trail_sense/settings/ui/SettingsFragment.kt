package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class SettingsFragment : AndromedaPreferenceFragment() {

    private val navigationMap = mapOf(
        R.string.pref_unit_settings to R.id.action_settings_to_unit_settings,
        R.string.pref_privacy_settings to R.id.action_settings_to_privacy_settings,
        R.string.pref_power_settings to R.id.action_settings_to_power_settings,
        R.string.pref_experimental_settings to R.id.action_settings_to_experimental_settings,
        R.string.pref_error_settings to R.id.action_settings_to_error_settings,
        R.string.pref_sensor_settings to R.id.action_settings_to_sensor_settings,

        // Tools
        R.string.pref_navigation_header_key to R.id.action_action_settings_to_navigationSettingsFragment,
        R.string.pref_weather_category to R.id.action_action_settings_to_weatherSettingsFragment,
        R.string.pref_astronomy_category to R.id.action_action_settings_to_astronomySettingsFragment,
        R.string.pref_flashlight_settings to R.id.action_action_settings_to_flashlightSettingsFragment,
        R.string.pref_maps_header_key to R.id.action_settings_to_map_settings,
        R.string.pref_tide_settings to R.id.action_settings_to_tide_settings,
        R.string.pref_clinometer_settings to R.id.action_settings_to_clinometer_settings,

        // About
        R.string.pref_open_source_licenses to R.id.action_action_settings_to_licenseFragment,
        R.string.pref_diagnostics to R.id.action_settings_to_diagnostics
    )

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        preference(R.string.pref_weather_category)?.isVisible = Sensors.hasBarometer(requireContext())

        preference(R.string.pref_flashlight_settings)?.isVisible = FlashlightSubsystem.getInstance(requireContext()).isAvailable()

        refreshOnChange(list(R.string.pref_theme))

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

        // TODO: Re-enable this if there are other experimental settings
        preference(R.string.pref_experimental_settings)?.isVisible = Sensors.hasCompass(requireContext()) &&
                Sensors.hasGyroscope(requireContext())
    }

    override fun onResume() {
        super.onResume()
        if (cache.getBoolean("pref_theme_just_changed") == true) {
            refresh(false)
        }
    }

    private fun refresh(recordChange: Boolean) {
        cache.putBoolean("pref_theme_just_changed", recordChange)
        activity?.recreate()
    }

    private fun refreshOnChange(pref: Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            refresh(true)
            true
        }
    }

}