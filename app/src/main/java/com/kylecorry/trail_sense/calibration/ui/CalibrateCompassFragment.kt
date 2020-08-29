package com.kylecorry.trail_sense.calibration.ui

import android.app.AlertDialog
import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.infrastructure.AltimeterCalibrator
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.sensors.declination.AutoDeclinationProvider
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
import com.kylecorry.trail_sense.shared.system.UiUtils
import kotlin.math.roundToInt


class CalibrateCompassFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private lateinit var azimuthTxt: Preference
    private lateinit var legacyCompassSwitch: SwitchPreferenceCompat
    private lateinit var compassSmoothingBar: SeekBarPreference
    private lateinit var declinationTxt: Preference
    private lateinit var trueNorthSwitch: SwitchPreferenceCompat
    private lateinit var autoDeclinationSwitch: SwitchPreferenceCompat
    private lateinit var declinationOverrideEdit: EditTextPreference
    private lateinit var declinationFromGpsBtn: Preference

    private lateinit var compass: ICompass
    private lateinit var declinationProvider: IDeclinationProvider
    private lateinit var realDeclinationProvider: IDeclinationProvider

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.compass_calibration, rootKey)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        compass = sensorService.getCompass()
        declinationProvider = sensorService.getDeclinationProvider()
        realDeclinationProvider =
            AutoDeclinationProvider(sensorService.getGPS(), sensorService.getAltimeter())

        bindPreferences()
    }

    private fun bindPreferences() {
        azimuthTxt = findPreference(getString(R.string.pref_holder_azimuth))!!
        legacyCompassSwitch = findPreference(getString(R.string.pref_use_legacy_compass))!!
        compassSmoothingBar = findPreference(getString(R.string.pref_compass_filter_amt))!!
        declinationTxt = findPreference(getString(R.string.pref_holder_declination))!!
        trueNorthSwitch = findPreference(getString(R.string.pref_use_true_north))!!
        autoDeclinationSwitch = findPreference(getString(R.string.pref_auto_declination))!!
        declinationOverrideEdit = findPreference(getString(R.string.pref_declination_override))!!
        declinationFromGpsBtn =
            findPreference(getString(R.string.pref_declination_override_gps_btn))!!

        declinationOverrideEdit.summary =
            getString(R.string.degree_format, prefs.declinationOverride.roundToInt())
        declinationOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL).or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }
        declinationOverrideEdit.isEnabled = prefs.navigation.useTrueNorth

        trueNorthSwitch.setOnPreferenceClickListener {
            declinationOverrideEdit.isEnabled = prefs.navigation.useTrueNorth
            resetCompass()
            true
        }

        autoDeclinationSwitch.setOnPreferenceClickListener {
            resetDeclinationProvider()
            true
        }

        declinationFromGpsBtn.setOnPreferenceClickListener {
            updateDeclinationFromGps()
            true
        }

        legacyCompassSwitch.setOnPreferenceClickListener {
            resetCompass()
            true
        }

        compassSmoothingBar.setOnPreferenceChangeListener { _, newValue ->
            compass.setSmoothing(newValue.toString().toIntOrNull() ?: 0)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        startCompass()
        startDeclination()
    }

    override fun onPause() {
        super.onPause()
        stopCompass()
        stopDeclination()
        realDeclinationProvider.stop(this::onUpdateDeclinationFromGpsCallback)
    }

    private fun updateDeclinationFromGps() {
        if (realDeclinationProvider.hasValidReading) {
            onUpdateDeclinationFromGpsCallback()
        } else {
            realDeclinationProvider.start(this::onUpdateDeclinationFromGpsCallback)
        }
    }

    private fun onUpdateDeclinationFromGpsCallback(): Boolean {
        prefs.declinationOverride = realDeclinationProvider.declination
        declinationOverrideEdit.text = realDeclinationProvider.declination.toString()
        UiUtils.shortToast(requireContext(), getString(R.string.declination_override_updated_toast))
        return false
    }

    private fun resetCompass() {
        stopCompass()
        compass = sensorService.getCompass()
        startCompass()
    }

    private fun resetDeclinationProvider() {
        stopDeclination()
        declinationProvider = sensorService.getDeclinationProvider()
        startDeclination()
    }

    private fun startDeclination() {
        declinationProvider.start(this::onDeclinationUpdate)
    }

    private fun stopDeclination() {
        declinationProvider.stop(this::onDeclinationUpdate)
    }

    private fun startCompass() {
        compass.start(this::onCompassUpdate)
    }

    private fun stopCompass() {
        compass.stop(this::onCompassUpdate)
    }

    private fun onDeclinationUpdate(): Boolean {
        update()
        return false
    }

    private fun onCompassUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        compass.declination = declinationProvider.declination

        azimuthTxt.summary = getString(R.string.degree_format, compass.bearing.value.roundToInt())
        declinationTxt.summary = getString(R.string.degree_format, compass.declination.roundToInt())
        declinationOverrideEdit.summary =
            getString(R.string.degree_format, prefs.declinationOverride.roundToInt())
    }


}