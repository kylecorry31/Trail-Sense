package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.text.InputType
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.sensors.ICompass
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.declination.AutoDeclinationProvider
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
import com.kylecorry.trail_sense.shared.system.UiUtils


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
    private lateinit var calibrateBtn: Preference

    private lateinit var compass: ICompass
    private lateinit var declinationProvider: IDeclinationProvider
    private lateinit var realDeclinationProvider: IDeclinationProvider

    private var prevAccuracy = Accuracy.Unknown

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
        calibrateBtn = findPreference(getString(R.string.pref_calibrate_compass_btn))!!

        declinationOverrideEdit.summary =
            getString(R.string.degree_format, prefs.declinationOverride)
        declinationOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }

        trueNorthSwitch.setOnPreferenceClickListener {
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

        calibrateBtn.setOnPreferenceClickListener {
            UiUtils.alert(
                requireContext(), getString(R.string.calibrate_compass_dialog_title), getString(
                    R.string.calibrate_compass_dialog_content, getString(R.string.dialog_ok)
                )
            )
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

        if (prevAccuracy != Accuracy.Unknown && prevAccuracy != compass.accuracy) {
            if (compass.accuracy.ordinal > prevAccuracy.ordinal) {
                UiUtils.shortToast(
                    requireContext(),
                    getString(R.string.compass_accuracy_improved, getCompassAccuracy())
                )
            }
            prevAccuracy = compass.accuracy
        }

        compass.declination = declinationProvider.declination

        calibrateBtn.summary = getString(R.string.compass_reported_accuracy, getCompassAccuracy())
        azimuthTxt.summary = getString(R.string.degree_format, compass.bearing.value)
        declinationTxt.summary = getString(R.string.degree_format, compass.declination)
        declinationOverrideEdit.summary =
            getString(R.string.degree_format, prefs.declinationOverride)
    }


    private fun getCompassAccuracy(): String {
        return when (compass.accuracy) {
            Accuracy.Low -> getString(R.string.accuracy_low)
            Accuracy.Medium -> getString(R.string.accuracy_medium)
            Accuracy.High -> getString(R.string.accuracy_high)
            Accuracy.Unknown -> getString(R.string.accuracy_unknown)
        }
    }


}