package com.kylecorry.trail_sense.licenses

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R


class LicenseFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.licenses, rootKey)

        val licenseSection = preferenceManager.findPreference<PreferenceCategory>(getString(R.string.pref_category_licenses))

        for (library in Licenses.libraries){
            val pref = Preference(requireContext())
            pref.title = library.name
            pref.summary = library.url
            pref.isIconSpaceReserved = false
            pref.isSingleLineTitle = false
            pref.setOnPreferenceClickListener {
                Alerts.dialog(
                    requireContext(),
                    library.name,
                    library.license,
                    cancelText = null
                )
                true
            }
            licenseSection?.addPreference(pref)
        }
    }



}