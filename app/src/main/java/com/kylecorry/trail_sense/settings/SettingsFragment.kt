package com.kylecorry.trail_sense.settings

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.RequestCodes
import com.kylecorry.trail_sense.shared.LowPowerMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.*
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils.getPackageName


class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var navController: NavController

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val sensorChecker = SensorChecker(requireContext())
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
            preference(R.string.pref_temperature_settings),
            R.id.action_action_settings_to_thermometerSettingsFragment
        )
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
        preference(R.string.pref_flashlight_settings)?.isVisible =
            Flashlight.hasFlashlight(requireContext())
        navigateOnClick(
            preference(R.string.pref_flashlight_settings),
            R.id.action_action_settings_to_flashlightSettingsFragment
        )
        navigateOnClick(
            preference(R.string.pref_cell_signal_settings),
            R.id.action_action_settings_to_cellSignalSettingsFragment
        )

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

        onClick(preference(R.string.pref_privacy_screenshot_protection)){
            ScreenUtils.setAllowScreenshots(requireActivity().window, !prefs.privacy.isScreenshotProtectionOn)
        }

        onClick(switch(R.string.pref_low_power_mode)) {
            if (prefs.isLowPowerModeOn) {
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

        navigateOnClick(
            findPreference(getString(R.string.pref_sensor_details)),
            R.id.action_action_settings_to_diagnosticFragment
        )

        onClick(switch(R.string.pref_enable_experimental)) {
            val pm: PackageManager? = context?.applicationContext?.packageManager
            val compName = ComponentName(
                getPackageName(requireContext()),
                getPackageName(requireContext()) + ".AliasMainActivity"
            )
            pm?.setComponentEnabledSetting(
                compName,
                if (prefs.experimentalEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Camera features
        onClick(switch(R.string.pref_use_camera_features)) {
            if (prefs.useCameraFeatures) {
                // TODO: Extract this to PermissionUtils for fragments
                // TODO: If previously denied, allow the user to open the settings
                requestPermissions(
                    listOf(Manifest.permission.CAMERA).toTypedArray(),
                    RequestCodes.REQUEST_CODE_CAMERA_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (!wasCameraPermissionGranted(requestCode)) {
            prefs.useCameraFeatures = false
            switch(R.string.pref_use_camera_features)?.isChecked = false
            UiUtils.longToast(requireContext(), getString(R.string.camera_permission_denied))
        }
    }

    private fun wasCameraPermissionGranted(requestCode: Int): Boolean {
        return requestCode == RequestCodes.REQUEST_CODE_CAMERA_PERMISSION && PermissionUtils.hasPermission(
            requireContext(),
            Manifest.permission.CAMERA
        )
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