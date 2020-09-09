package com.kylecorry.trail_sense.calibration.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.infrastructure.AltimeterCalibrator
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.system.UiUtils


class CalibrateAltimeterFragment : PreferenceFragmentCompat() {

    private lateinit var barometer: IBarometer
    private lateinit var altimeterCalibrator: AltimeterCalibrator
    private lateinit var gps: IGPS
    private lateinit var altimeter: IAltimeter
    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private var altimeterStarted = false
    private lateinit var distanceUnits: UserPreferences.DistanceUnits

    private lateinit var altitudeTxt: Preference
    private lateinit var autoAltitudeSwitch: SwitchPreferenceCompat
    private lateinit var elevationCorrectionSwitch: SwitchPreferenceCompat
    private lateinit var fineTuneSwitch: SwitchPreferenceCompat
    private lateinit var altitudeOverrideEdit: EditTextPreference
    private lateinit var altitudeOverrideFeetEdit: EditTextPreference
    private lateinit var altitudeOverrideGpsBtn: Preference
    private lateinit var altitudeOverrideBarometerEdit: EditTextPreference

    private var seaLevelPressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.altimeter_calibration, rootKey)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        gps = GPS(requireContext())
        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()

        altimeterCalibrator = AltimeterCalibrator(requireContext())

        distanceUnits = prefs.distanceUnits

        bindPreferences()
    }

    private fun bindPreferences(){
        altitudeTxt = findPreference(getString(R.string.pref_holder_altitude))!!
        autoAltitudeSwitch = findPreference(getString(R.string.pref_auto_altitude))!!
        elevationCorrectionSwitch = findPreference(getString(R.string.pref_altitude_offsets))!!
        fineTuneSwitch = findPreference(getString(R.string.pref_fine_tune_altitude))!!
        altitudeOverrideEdit = findPreference(getString(R.string.pref_altitude_override))!!
        altitudeOverrideFeetEdit = findPreference(getString(R.string.pref_altitude_override_feet))!!
        altitudeOverrideGpsBtn = findPreference(getString(R.string.pref_altitude_from_gps_btn))!!
        altitudeOverrideBarometerEdit = findPreference(getString(R.string.pref_altitude_override_sea_level))!!

        altitudeOverrideEdit.summary = getAltitudeOverrideString()
        altitudeOverrideFeetEdit.summary = getAltitudeOverrideFeetString()
        altitudeOverrideFeetEdit.isEnabled = !prefs.useAutoAltitude
        altitudeOverrideEdit.isEnabled = !prefs.useAutoAltitude
        altitudeOverrideGpsBtn.isEnabled = !prefs.useAutoAltitude
        altitudeOverrideBarometerEdit.isEnabled = !prefs.useAutoAltitude
        altitudeOverrideBarometerEdit.isVisible = prefs.weather.hasBarometer
        fineTuneSwitch.isVisible = prefs.weather.hasBarometer
        altitudeOverrideBarometerEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL).or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }
        altitudeOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL).or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }
        altitudeOverrideFeetEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL).or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }

        altitudeOverrideGpsBtn.setOnPreferenceClickListener {
            updateElevationFromGPS()
            true
        }

        autoAltitudeSwitch.setOnPreferenceClickListener {
            restartAltimeter()
            altitudeOverrideFeetEdit.isEnabled = !prefs.useAutoAltitude
            altitudeOverrideEdit.isEnabled = !prefs.useAutoAltitude
            altitudeOverrideGpsBtn.isEnabled = !prefs.useAutoAltitude
            altitudeOverrideBarometerEdit.isEnabled = !prefs.useAutoAltitude
            true
        }

        fineTuneSwitch.setOnPreferenceClickListener {
            restartAltimeter()
            true
        }

        elevationCorrectionSwitch.setOnPreferenceClickListener {
            restartAltimeter()
            true
        }

        if (distanceUnits == UserPreferences.DistanceUnits.Feet){
            altitudeOverrideEdit.isVisible = false
            altitudeOverrideFeetEdit.isVisible = true
        } else {
            altitudeOverrideFeetEdit.isVisible = false
            altitudeOverrideEdit.isVisible = true
        }

        altitudeOverrideBarometerEdit.setOnPreferenceChangeListener { _, newValue ->
            updateElevationFromBarometer(newValue.toString().toFloatOrNull() ?: 0.0f)
            true
        }


        if (altitudeOverrideFeetEdit.isEnabled) {
            altitudeOverrideFeetEdit.setOnPreferenceChangeListener { _, newValue ->
                prefs.altitudeOverride = LocationMath.convertToMeters(
                    newValue.toString().toFloatOrNull() ?: 0.0f,
                    UserPreferences.DistanceUnits.Feet
                )
                updateAltitude()
                true
            }
        }

        if (altitudeOverrideEdit.isEnabled) {
            altitudeOverrideEdit.setOnPreferenceChangeListener { _, newValue ->
                preferenceManager.sharedPreferences.edit {
                    putString(
                        "pref_altitude_override_feet",
                        LocationMath.convertToBaseUnit(
                            newValue.toString().toFloatOrNull() ?: 0f,
                            UserPreferences.DistanceUnits.Feet
                        ).toString()
                    )
                }
                updateAltitude()
                true
            }
        }
    }

    private fun getAltitudeOverrideFeetString(): String {
        return LocationMath.convertToBaseUnit(
            prefs.altitudeOverride,
            UserPreferences.DistanceUnits.Feet
        ).roundPlaces(1).toString() + " ft"
    }

    private fun getAltitudeOverrideString(): String {
        return prefs.altitudeOverride.roundPlaces(1).toString() + " m"
    }

    private fun getAltitudeString(): String {
        return LocationMath.convertToBaseUnit(
            altimeter.altitude,
            distanceUnits
        ).roundPlaces(1).toString() + if (distanceUnits == UserPreferences.DistanceUnits.Feet) " ft" else " m"
    }

    private fun restartAltimeter(){
        stopAltimeter()
        altimeter = sensorService.getAltimeter()
        startAltimeter()
        updateAltitude()
    }

    override fun onResume() {
        super.onResume()
        startAltimeter()
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::onElevationFromBarometerCallback)
        gps.stop(this::onElevationFromGPSCallback)
        stopAltimeter()
    }

    private fun updateElevationFromGPS() {
        gps.start(this::onElevationFromGPSCallback)
    }

    private fun onElevationFromGPSCallback(): Boolean {
        val elevation = gps.altitude
        prefs.altitudeOverride = elevation
        preferenceManager.sharedPreferences.edit {
            putString(
                getString(R.string.pref_altitude_override_feet),
                LocationMath.convertToBaseUnit(
                    prefs.altitudeOverride,
                    UserPreferences.DistanceUnits.Feet
                ).toString()
            )
        }
        updateAltitude()
        UiUtils.shortToast(requireContext(), getString(R.string.altitude_override_updated_toast))
        return false
    }

    private fun startAltimeter() {
        if (altimeterStarted) {
            return
        }
        altimeterStarted = true
        altimeter.start(this::updateAltitude)
    }

    private fun stopAltimeter() {
        altimeterStarted = false
        altimeter.stop(this::updateAltitude)
    }

    private fun updateElevationFromBarometer(seaLevelPressure: Float) {
        this.seaLevelPressure = seaLevelPressure
        barometer.start(this::onElevationFromBarometerCallback)
    }

    private fun onElevationFromBarometerCallback(): Boolean {
        val elevation = SensorManager.getAltitude(seaLevelPressure, barometer.pressure)
        prefs.altitudeOverride = elevation
        preferenceManager.sharedPreferences.edit {
            putString(
                getString(R.string.pref_altitude_override_feet),
                LocationMath.convertToBaseUnit(
                    prefs.altitudeOverride,
                    UserPreferences.DistanceUnits.Feet
                ).toString()
            )
        }
        updateAltitude()
        UiUtils.shortToast(requireContext(), getString(R.string.altitude_override_updated_toast))
        return false
    }

    private fun updateAltitude(): Boolean {

        if (throttle.isThrottled()) {
            return true
        }

        altitudeTxt.summary = getAltitudeString()

        altitudeOverrideFeetEdit.summary = getAltitudeOverrideFeetString()
        altitudeOverrideEdit.summary = getAltitudeOverrideString()

        return true
    }


}