package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.locationformat.LocationDecimalDegreesFormatter
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle


class CalibrateGPSFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private lateinit var locationTxt: Preference
    private lateinit var autoLocationSwitch: SwitchPreferenceCompat
    private lateinit var latOverrideEdit: EditTextPreference
    private lateinit var lngOverrideEdit: EditTextPreference
    private lateinit var fromGpsBtn: Preference
    private lateinit var permissionBtn: Preference
    private val formatter = LocationDecimalDegreesFormatter()

    private lateinit var gps: IGPS
    private lateinit var realGps: GPS

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gps_calibration, rootKey)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        gps = sensorService.getGPS()
        realGps = GPS(requireContext().applicationContext)

        bindPreferences()
    }

    private fun bindPreferences() {
        locationTxt = findPreference(getString(R.string.pref_holder_location))!!
        autoLocationSwitch = findPreference(getString(R.string.pref_auto_location))!!
        latOverrideEdit = findPreference(getString(R.string.pref_latitude_override))!!
        lngOverrideEdit = findPreference(getString(R.string.pref_longitude_override))!!
        fromGpsBtn = findPreference(getString(R.string.pref_gps_override_btn))!!
        permissionBtn = findPreference(getString(R.string.pref_gps_request_permission))!!

        latOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType =
                InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL).or(
                    InputType.TYPE_NUMBER_FLAG_SIGNED
                )
        }

        lngOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }

        autoLocationSwitch.setOnPreferenceClickListener {
            latOverrideEdit.isEnabled = !prefs.useLocationFeatures || !prefs.useAutoLocation
            lngOverrideEdit.isEnabled = !prefs.useLocationFeatures || !prefs.useAutoLocation
            fromGpsBtn.isEnabled = !prefs.useLocationFeatures || !prefs.useAutoLocation
            resetGPS()
            update()
            true
        }

        fromGpsBtn.setOnPreferenceClickListener {
            realGps.start(this::setOverrideFromGPS)
            true
        }

        permissionBtn.setOnPreferenceClickListener {
            val intent = IntentUtils.appSettings(requireContext())
            startActivityForResult(intent, 1000)
            true
        }

        // TODO: Validate lat lng input
    }

    override fun onResume() {
        super.onResume()
        if (gps.hasValidReading) {
            update()
        }
        startGPS()
    }

    override fun onPause() {
        super.onPause()
        stopGPS()
        realGps.stop(this::setOverrideFromGPS)
    }

    private fun resetGPS() {
        stopGPS()
        gps = sensorService.getGPS()
        startGPS()
    }

    private fun setOverrideFromGPS(): Boolean {
        prefs.locationOverride = realGps.location
        update()
        return false
    }

    private fun startGPS() {
        gps.start(this::onLocationUpdate)
    }

    private fun stopGPS() {
        gps.stop(this::onLocationUpdate)
    }


    private fun onLocationUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        permissionBtn.isVisible = !prefs.useLocationFeatures
        autoLocationSwitch.isEnabled = prefs.useLocationFeatures
        latOverrideEdit.isEnabled = !prefs.useLocationFeatures || !prefs.useAutoLocation
        lngOverrideEdit.isEnabled = !prefs.useLocationFeatures || !prefs.useAutoLocation
        fromGpsBtn.isEnabled = !prefs.useLocationFeatures || !prefs.useAutoLocation

        locationTxt.summary = getString(
            R.string.coordinate_format_string_dd,
            formatter.formatLatitude(gps.location),
            formatter.formatLongitude(gps.location)
        )
        val overrides = prefs.locationOverride
        latOverrideEdit.summary = overrides.latitude.toString()
        lngOverrideEdit.summary = overrides.longitude.toString()
    }


}