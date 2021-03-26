package com.kylecorry.trail_sense.settings

import android.content.Intent
import android.hardware.Sensor
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.LowPowerMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorDetailProvider
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var navController: NavController
    private var prefMaxBeaconDistanceKm: EditTextPreference? = null
    private var prefMaxBeaconDistanceMi: EditTextPreference? = null
    private val intervalometer = Intervalometer {
        updatePreferenceStates()
    }

    private val prefs by lazy { UserPreferences(requireContext()) }

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
        bindPreferences()
        updatePreferenceStates()
        if (!sensorChecker.hasBarometer()) {
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_weather_category))
            preference(R.string.pref_barometer_calibration)?.title =
                getString(R.string.tool_thermometer_title)
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
        preference(R.string.pref_odometer_calibration)?.isVisible = prefs.experimentalEnabled && sensorChecker.hasSensor(Sensor.TYPE_STEP_COUNTER)
        navigateOnClick(
            preference(R.string.pref_odometer_calibration),
            R.id.action_action_settings_to_calibrateOdometerFragment
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

        onClick(preference(R.string.pref_enable_experimental)){
            preference(R.string.pref_odometer_calibration)?.isVisible = prefs.experimentalEnabled && sensorChecker.hasSensor(Sensor.TYPE_STEP_COUNTER)
            if (!prefs.experimentalEnabled){
                PedometerService.stop(requireContext())
                WhiteNoiseService.stop(requireContext())
            } else {
                if (prefs.usePedometer){
                    PedometerService.start(requireContext())
                }
            }
        }

        refreshOnChange(list(R.string.pref_theme))

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_ruler_calibration))
            ?.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }

        navigateOnClick(
            preferenceScreen.findPreference(getString(R.string.pref_open_source_licenses)),
            R.id.action_action_settings_to_licenseFragment
        )

        onClick(switch(R.string.pref_low_power_mode)){
            if (prefs.isLowPowerModeOn){
                LowPowerMode(requireContext()).enable(requireActivity())
            } else {
                LowPowerMode(requireContext()).disable(requireActivity())
            }
        }

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

        navigateOnClick(findPreference(getString(R.string.pref_sensor_details)), R.id.action_action_settings_to_diagnosticFragment)
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

}