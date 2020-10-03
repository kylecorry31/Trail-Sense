package com.kylecorry.trail_sense

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.licenses.LicenseFragment
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class SettingsFragment : PreferenceFragmentCompat() {

    private var prefMonitorWeather: SwitchPreferenceCompat? = null
    private var prefWeatherUpdateFrequency: ListPreference? = null
    private var prefUpdateWeatherForeground: SwitchPreferenceCompat? = null
    private var prefForceWeatherUpdates: SwitchPreferenceCompat? = null
    private var prefShowWeatherNotification: SwitchPreferenceCompat? = null
    private var prefShowPressureInNotification: SwitchPreferenceCompat? = null
    private lateinit var navController: NavController
    private var prefPressureHistory: ListPreference? = null
    private var prefStormAlerts: SwitchPreferenceCompat? = null
    private var prefMaxBeaconDistanceKm: EditTextPreference? = null
    private var prefMaxBeaconDistanceMi: EditTextPreference? = null
    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val intervalometer = Intervalometer {
        updatePreferenceStates()
    }

    private lateinit var prefs: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
    }

    private fun bindPreferences() {
        prefMonitorWeather = switch(R.string.pref_monitor_weather)
        prefWeatherUpdateFrequency = list(R.string.pref_weather_update_frequency)
        prefUpdateWeatherForeground = switch(R.string.pref_weather_foreground_service)
        prefForceWeatherUpdates = switch(R.string.pref_force_weather_updates)
        prefShowWeatherNotification = switch(R.string.pref_show_weather_notification)
        prefShowPressureInNotification = switch(R.string.pref_show_pressure_in_notification)
        prefPressureHistory = list(R.string.pref_pressure_history)
        prefStormAlerts = switch(R.string.pref_send_storm_alert)
        prefMaxBeaconDistanceKm = editText(R.string.pref_max_beacon_distance)
        prefMaxBeaconDistanceMi = editText(R.string.pref_max_beacon_distance_miles)
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val sensorChecker = SensorChecker(requireContext())
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()
        updatePreferenceStates()
        if (!sensorChecker.hasBarometer()) {
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_weather_category))
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_barometer_calibration))
        }

        prefMonitorWeather?.setOnPreferenceClickListener {
            if (prefs.weather.shouldMonitorWeather) {
                WeatherUpdateScheduler.start(requireContext())
            } else {
                WeatherUpdateScheduler.stop(requireContext())
            }
            updatePreferenceStates()
            true
        }
        prefUpdateWeatherForeground?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            updatePreferenceStates()
            true
        }
        prefForceWeatherUpdates?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            updatePreferenceStates()
            true
        }
        prefShowWeatherNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            updatePreferenceStates()
            true
        }
        prefWeatherUpdateFrequency?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        navigateOnClick(
            preference(R.string.pref_compass_sensor),
            R.id.action_action_settings_to_calibrateCompassFragment
        )
        navigateOnClick(
            preference(R.string.pref_altimeter_calibration),
            R.id.action_action_settings_to_calibrateAltimeterFragment
        )
        navigateOnClick(
            preference(R.string.pref_gps_calibration),
            R.id.action_action_settings_to_calibrateGPSFragment
        )
        navigateOnClick(
            preference(R.string.pref_barometer_calibration),
            R.id.action_action_settings_to_calibrateBarometerFragment
        )
        refreshOnChange(list(R.string.pref_theme))
        refreshOnChange(switch(R.string.pref_enable_experimental))

        val maxDistance = prefs.navigation.maxBeaconDistance
        prefMaxBeaconDistanceMi?.summary = formatService.formatDistance(
            unitService.convert(
                maxDistance,
                DistanceUnits.Meters,
                DistanceUnits.Miles
            ), DistanceUnits.Miles
        )
        prefMaxBeaconDistanceKm?.summary = formatService.formatDistance(
            unitService.convert(
                maxDistance,
                DistanceUnits.Meters,
                DistanceUnits.Kilometers
            ), DistanceUnits.Kilometers
        )

        prefMaxBeaconDistanceMi?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        prefMaxBeaconDistanceKm?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        prefMaxBeaconDistanceMi?.setOnPreferenceChangeListener { _, newValue ->
            val miles = newValue.toString().toFloatOrNull() ?: 62f
            prefs.navigation.maxBeaconDistance =
                unitService.convert(miles, DistanceUnits.Miles, DistanceUnits.Meters)
            prefMaxBeaconDistanceMi?.summary =
                formatService.formatDistance(miles, DistanceUnits.Miles)
            prefMaxBeaconDistanceKm?.summary = formatService.formatDistance(
                unitService.convert(
                    miles,
                    DistanceUnits.Miles,
                    DistanceUnits.Kilometers
                ), DistanceUnits.Kilometers
            )
            true
        }

        prefMaxBeaconDistanceKm?.setOnPreferenceChangeListener { _, newValue ->
            val km = newValue.toString().toFloatOrNull() ?: 100f
            preferenceManager.sharedPreferences.edit {
                putString(
                    getString(R.string.pref_max_beacon_distance_miles),
                    unitService.convert(km, DistanceUnits.Kilometers, DistanceUnits.Miles)
                        .toString()
                )
            }
            prefMaxBeaconDistanceMi?.summary = formatService.formatDistance(
                unitService.convert(
                    km,
                    DistanceUnits.Kilometers,
                    DistanceUnits.Miles
                ), DistanceUnits.Miles
            )
            prefMaxBeaconDistanceKm?.summary =
                formatService.formatDistance(km, DistanceUnits.Kilometers)
            true
        }

        prefShowPressureInNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_sunset_alert_time))
            ?.setOnPreferenceClickListener { _ ->
                context?.apply {
                    sendBroadcast(SunsetAlarmReceiver.intent(this))
                }
                true
            }

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_ruler_calibration))
            ?.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_num_visible_beacons))
            ?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

        navigateOnClick(preferenceScreen.findPreference(getString(R.string.pref_open_source_licenses)), R.id.action_action_settings_to_licenseFragment)

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_github))
            ?.setOnPreferenceClickListener {
                val i = IntentUtils.url(it.summary.toString())
                startActivity(i)
                true
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_email))
            ?.setOnPreferenceClickListener {

                val intent = IntentUtils.email(it.summary.toString(), getString(R.string.app_name))
                startActivity(Intent.createChooser(intent, it.title.toString()))
                true
            }

        val forecastSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_forecast_sensitivity))
        forecastSensitivity?.setEntries(getForecastSensitivityArray(userPrefs.pressureUnits))

        val stormSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_storm_alert_sensitivity))
        stormSensitivity?.setEntries(getStormSensitivityArray(userPrefs.pressureUnits))

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_pressure_units))
            ?.setOnPreferenceChangeListener { _, newValue ->
                val mapped = when (newValue) {
                    "hpa" -> PressureUnits.Hpa
                    "mbar" -> PressureUnits.Mbar
                    "in" -> PressureUnits.Inhg
                    else -> PressureUnits.Psi
                }

                forecastSensitivity?.setEntries(getForecastSensitivityArray(mapped))
                stormSensitivity?.setEntries(getStormSensitivityArray(mapped))

                true
            }


        val version = PackageUtils.getVersionName(requireContext())
        preferenceScreen.findPreference<Preference>(getString(R.string.pref_app_version))?.summary =
            version
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(1000)
    }

    override fun onPause() {
        intervalometer.stop()
        super.onPause()
    }

    private fun navigateOnClick(pref: Preference?, @IdRes action: Int, bundle: Bundle? = null) {
        pref?.setOnPreferenceClickListener {
            navController.navigate(action, bundle)
            false
        }
    }

    private fun refreshOnChange(pref: Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }
    }

    private fun restartWeatherMonitor() {
        WeatherUpdateScheduler.stop(requireContext())
        WeatherUpdateScheduler.start(requireContext())
    }

    private fun updatePreferenceStates() {
        val distanceUnits = prefs.distanceUnits

        if (distanceUnits == UserPreferences.DistanceUnits.Feet) {
            prefMaxBeaconDistanceKm?.isVisible = false
            prefMaxBeaconDistanceMi?.isVisible = true
        } else {
            prefMaxBeaconDistanceKm?.isVisible = true
            prefMaxBeaconDistanceMi?.isVisible = false
        }
    }

    private fun switch(@StringRes id: Int): SwitchPreferenceCompat? {
        return preferenceManager.findPreference(getString(id))
    }

    private fun editText(@StringRes id: Int): EditTextPreference? {
        return preferenceManager.findPreference(getString(id))
    }

    private fun list(@StringRes id: Int): ListPreference? {
        return preferenceManager.findPreference(getString(id))
    }

    private fun preference(@StringRes id: Int): Preference? {
        return preferenceManager.findPreference(getString(id))
    }

    @ArrayRes
    private fun getForecastSensitivityArray(units: PressureUnits): Int {
        return when (units) {
            PressureUnits.Hpa -> R.array.forecast_sensitivity_entries_hpa
            PressureUnits.Inhg -> R.array.forecast_sensitivity_entries_in
            PressureUnits.Psi -> R.array.forecast_sensitivity_entries_psi
            else -> R.array.forecast_sensitivity_entries_mbar
        }
    }

    @ArrayRes
    private fun getStormSensitivityArray(units: PressureUnits): Int {
        return when (units) {
            PressureUnits.Hpa -> R.array.storm_sensitivity_entries_hpa
            PressureUnits.Inhg -> R.array.storm_sensitivity_entries_in
            PressureUnits.Psi -> R.array.storm_sensitivity_entries_psi
            else -> R.array.storm_sensitivity_entries_mbar
        }
    }

}