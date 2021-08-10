package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class SettingsFragment : CustomPreferenceFragment() {

    private val navigationMap = mapOf(
        R.string.pref_unit_settings to R.id.action_settings_to_unit_settings,
        R.string.pref_privacy_settings to R.id.action_settings_to_privacy_settings,
        R.string.pref_power_settings to R.id.action_settings_to_power_settings,
        R.string.pref_experimental_settings to R.id.action_settings_to_experimental_settings,
        R.string.pref_sensor_settings to R.id.action_settings_to_sensor_settings,

        // Tools
        R.string.pref_navigation_header_key to R.id.action_action_settings_to_navigationSettingsFragment,
        R.string.pref_weather_category to R.id.action_action_settings_to_weatherSettingsFragment,
        R.string.pref_astronomy_category to R.id.action_action_settings_to_astronomySettingsFragment,
        R.string.pref_flashlight_settings to R.id.action_action_settings_to_flashlightSettingsFragment,
        R.string.pref_maps_header_key to R.id.action_settings_to_map_settings,

        // About
        R.string.pref_open_source_licenses to R.id.action_action_settings_to_licenseFragment
    )

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        val sensorChecker = SensorChecker(requireContext())

        preference(R.string.pref_maps_header_key)?.isVisible = prefs.navigation.areMapsEnabled
        preference(R.string.pref_weather_category)?.isVisible = sensorChecker.hasBarometer()

        preference(R.string.pref_flashlight_settings)?.isVisible =
            Torch.isAvailable(requireContext())

        refreshOnChange(list(R.string.pref_theme))

        onClick(preference(R.string.pref_github)) {
            val i = IntentUtils.url(it.summary.toString())
            startActivity(i)
        }

        onClick(preference(R.string.pref_privacy_policy)) {
            val i = IntentUtils.url(it.summary.toString())
            startActivity(i)
        }

        onClick(preference(R.string.pref_email)) {
            val intent = IntentUtils.email(it.summary.toString(), getString(R.string.app_name))
            startActivity(Intent.createChooser(intent, it.title.toString()))
        }

        val version = PackageUtils.getVersionName(requireContext())
        preference(R.string.pref_app_version)?.summary = version
        setIconColor(preferenceScreen, UiUtils.androidTextColorSecondary(requireContext()))
    }

    override fun onResume() {
        super.onResume()
        preference(R.string.pref_maps_header_key)?.isVisible = prefs.navigation.areMapsEnabled
    }

    private fun refreshOnChange(pref: Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }
    }

}