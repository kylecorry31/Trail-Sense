package com.kylecorry.trail_sense.settings

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
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorDetailProvider
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var navController: NavController
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
        navigateOnClick(
            preference(R.string.pref_navigation_header_key),
            R.id.action_action_settings_to_navigationSettingsFragment
        )
        navigateOnClick(
            preference(R.string.pref_weather_category),
            R.id.action_action_settings_to_weatherSettingsFragment
        )
        navigateOnClick(
            preference(R.string.pref_astronomy_category),
            R.id.action_action_settings_to_astronomySettingsFragment
        )
        refreshOnChange(list(R.string.pref_theme))
        refreshOnChange(switch(R.string.pref_enable_experimental))

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_ruler_calibration))
            ?.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }

        navigateOnClick(
            preferenceScreen.findPreference(getString(R.string.pref_open_source_licenses)),
            R.id.action_action_settings_to_licenseFragment
        )

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_github))
            ?.setOnPreferenceClickListener {
                val i = IntentUtils.url(it.summary.toString())
                startActivity(i)
                true
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_privacy_policy))
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

        val version = PackageUtils.getVersionName(requireContext())
        preferenceScreen.findPreference<Preference>(getString(R.string.pref_app_version))?.summary =
            version

        onClick(findPreference(getString(R.string.pref_sensor_details))) {
            UiUtils.alert(
                requireContext(),
                getString(R.string.pref_sensor_details_title),
                SensorDetailProvider().getSensorDetails(requireContext()),
                getString(R.string.dialog_ok)
            )
        }
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

    private fun onClick(pref: Preference?, action: () -> Unit) {
        pref?.setOnPreferenceClickListener {
            action()
            true
        }
    }

    private fun refreshOnChange(pref: Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }
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