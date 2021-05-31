package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import com.kylecorry.trailsensecore.infrastructure.system.ScreenUtils

class PrivacySettingsFragment : CustomPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.privacy_preferences, rootKey)

        onClick(preference(R.string.pref_privacy_screenshot_protection)) {
            ScreenUtils.setAllowScreenshots(
                requireActivity().window,
                !prefs.privacy.isScreenshotProtectionOn
            )
        }
        navigateOnClick(
            preference(R.string.pref_gps_calibration),
            R.id.action_privacy_settings_to_gps_calibration
        )
    }

    override fun onResume() {
        super.onResume()
        preference(R.string.pref_gps_calibration)?.summary = if (isLocationMocked()) {
            getString(R.string.location_mocked)
        } else {
            getString(R.string.location_not_mocked)
        }
    }

    private fun isLocationMocked(): Boolean {
        return !prefs.useAutoLocation || !PermissionUtils.isLocationEnabled(requireContext())
    }

}