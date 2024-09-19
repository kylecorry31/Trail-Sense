package com.kylecorry.trail_sense.settings.licenses

import android.os.Bundle
import android.text.util.Linkify.WEB_URLS
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R


class LicenseFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.licenses, rootKey)

        val licenseSection =
            preferenceManager.findPreference<PreferenceCategory>(getString(R.string.pref_category_licenses))

        for (library in Licenses.libraries) {
            val pref = Preference(requireContext())
            pref.title = library.name
            pref.summary = library.url
            pref.isIconSpaceReserved = false
            pref.isSingleLineTitle = false
            pref.setOnPreferenceClickListener {
                val content = (library.url + "\n\n" + library.license()).toSpannable()
                LinkifyCompat.addLinks(content, WEB_URLS)

                Alerts.dialog(
                    requireContext(),
                    library.name,
                    content,
                    cancelText = null,
                    allowLinks = true
                )
                true
            }
            licenseSection?.addPreference(pref)
        }
    }


}