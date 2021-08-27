package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.kylecorry.andromeda.core.math.toFloatCompat
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.andromeda.core.units.Temperature
import com.kylecorry.andromeda.core.units.TemperatureUnits

class ThermometerSettingsFragment : AndromedaPreferenceFragment() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }

    private var temperatureTxt: Preference? = null
    private var minTempCalibratedC: EditTextPreference? = null
    private var maxTempCalibratedC: EditTextPreference? = null
    private var minTempUncalibratedC: EditTextPreference? = null
    private var maxTempUncalibratedC: EditTextPreference? = null
    private var minTempCalibratedF: EditTextPreference? = null
    private var maxTempCalibratedF: EditTextPreference? = null
    private var minTempUncalibratedF: EditTextPreference? = null
    private var maxTempUncalibratedF: EditTextPreference? = null


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.thermometer_settings, rootKey)
        setIconColor(Resources.androidTextColorSecondary(requireContext()))

        minTempCalibratedC = editText(R.string.pref_min_calibrated_temp_c)
        maxTempCalibratedC = editText(R.string.pref_max_calibrated_temp_c)
        minTempUncalibratedC = editText(R.string.pref_min_uncalibrated_temp_c)
        maxTempUncalibratedC = editText(R.string.pref_max_uncalibrated_temp_c)
        minTempCalibratedF = editText(R.string.pref_min_calibrated_temp_f)
        maxTempCalibratedF = editText(R.string.pref_max_calibrated_temp_f)
        minTempUncalibratedF = editText(R.string.pref_min_uncalibrated_temp_f)
        maxTempUncalibratedF = editText(R.string.pref_max_uncalibrated_temp_f)
        temperatureTxt = preference(R.string.pref_temperature_holder)

        if (prefs.temperatureUnits == TemperatureUnits.C) {
            minTempCalibratedF?.isVisible = false
            maxTempCalibratedF?.isVisible = false
            minTempUncalibratedF?.isVisible = false
            maxTempUncalibratedF?.isVisible = false
            minTempCalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.minActualTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
            minTempUncalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.minBatteryTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
            maxTempCalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.maxActualTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
            maxTempUncalibratedC?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.maxBatteryTemperatureF =
                    Temperature(temp, TemperatureUnits.C).convertTo(TemperatureUnits.F).temperature
                true
            }
        } else {
            minTempCalibratedC?.isVisible = false
            maxTempCalibratedC?.isVisible = false
            minTempUncalibratedC?.isVisible = false
            maxTempUncalibratedC?.isVisible = false
            minTempCalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.minActualTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
            minTempUncalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.minBatteryTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
            maxTempCalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.maxActualTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
            maxTempUncalibratedF?.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat() ?: 0f
                prefs.weather.maxBatteryTemperature =
                    Temperature(temp, TemperatureUnits.F).celsius().temperature
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        thermometer.asLiveData().observe(viewLifecycleOwner, {
            temperatureTxt?.summary =
                formatService.formatTemperature(
                    Temperature(
                        thermometer.temperature,
                        TemperatureUnits.C
                    ).convertTo(prefs.temperatureUnits)
                )
        })
    }

}