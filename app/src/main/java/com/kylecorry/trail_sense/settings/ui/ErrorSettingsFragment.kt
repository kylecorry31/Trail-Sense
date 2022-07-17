package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ErrorBannerReason

class ErrorSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.error_preferences, rootKey)

        val errors = ErrorBannerReason.values().sortedBy { it.id }

        val bannerCategory =
            findPreference<PreferenceCategory>(getString(R.string.pref_error_banner_category))

        for (error in errors) {
            val pref = SwitchPreferenceCompat(requireContext())
            pref.title = getDisplayName(error)
            pref.isSingleLineTitle = false
            pref.isIconSpaceReserved = false
            pref.key = "pref_can_show_error_${error.id}"
            pref.setDefaultValue(true)
            bannerCategory?.addPreference(pref)
        }
    }

    private fun getDisplayName(reason: ErrorBannerReason): String {
        return when (reason) {
            ErrorBannerReason.NoGPS -> getString(R.string.gps_unavailable)
            ErrorBannerReason.LocationNotSet -> getString(R.string.location_not_set)
            ErrorBannerReason.CompassPoor -> getString(R.string.compass_accuracy)
            ErrorBannerReason.NoCompass -> getString(R.string.compass_unavailable)
            ErrorBannerReason.GPSTimeout -> getString(R.string.gps_timeouts)
        }
    }

}