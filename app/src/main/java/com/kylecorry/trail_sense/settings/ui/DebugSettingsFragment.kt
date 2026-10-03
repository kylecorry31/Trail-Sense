package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.preference.Preference
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherCsvConverter
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo

class DebugSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences, rootKey)

        findPreference<Preference>("pref_debug_import_weather")?.setOnPreferenceClickListener {
            importWeather()
            true
        }
    }

    private fun importWeather() {
        val importer = IOFactory().createCsvService(requireMainActivity())
        val repo = WeatherRepo.getInstance(requireContext())
        inBackground(BackgroundMinimumState.Created) {
            val csv = importer.import() ?: return@inBackground
            val readings = WeatherCsvConverter().fromCSV(csv)
            if (readings.isNotEmpty()) {
                repo.replaceAll(readings)
            }
            onMain {
                Alerts.toast(
                    requireContext(),
                    if (readings.isEmpty()) "No weather readings found"
                    else "Imported ${readings.size} weather readings"
                )
            }
        }
    }
}
