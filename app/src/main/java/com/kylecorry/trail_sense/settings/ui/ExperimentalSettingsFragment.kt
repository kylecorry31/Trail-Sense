package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

class ExperimentalSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)

        val sensors = SensorService(requireContext())
        val hasGyro = Sensors.hasGyroscope(requireContext())
        val hasCompass = sensors.hasCompass()

        preference(R.string.pref_experimental_metal_direction)?.isVisible = hasGyro && hasCompass

        onClick(preference(R.string.pref_cliff_height_enabled)) {
            requireMainActivity().updateBottomNavigation()
        }

        val sources = ForecastSource.entries
        val sourceNames = sources.map { it.name }
        val idMap = mapOf(
            ForecastSource.Sol to 1,
            ForecastSource.Zambretti to 2,
        )
        val sourceIds = sources.map { (idMap[it] ?: 0).toString() }

        val forecastSource = list(R.string.pref_weather_forecast_source)

        forecastSource?.entries = sourceNames.toTypedArray()
        forecastSource?.entryValues = sourceIds.toTypedArray()
        forecastSource?.isVisible =
            Tools.isToolAvailable(requireContext(), Tools.WEATHER) && isDebug()
    }
}