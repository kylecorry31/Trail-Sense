package com.kylecorry.trail_sense.calibration.ui

import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import androidx.core.content.edit
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.Instant


class CalibrateAltimeterFragment : PreferenceFragmentCompat() {

    private lateinit var barometer: IBarometer
    private lateinit var gps: IGPS
    private lateinit var altimeter: IAltimeter
    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private var altimeterStarted = false
    private lateinit var distanceUnits: UserPreferences.DistanceUnits

    private lateinit var altitudeTxt: Preference
    private lateinit var calibrationModeList: ListPreference
    private lateinit var elevationCorrectionSwitch: SwitchPreferenceCompat
    private lateinit var altitudeOverrideEdit: EditTextPreference
    private lateinit var altitudeOverrideFeetEdit: EditTextPreference
    private lateinit var altitudeOverrideGpsBtn: Preference
    private lateinit var altitudeOverrideBarometerEdit: EditTextPreference

    private lateinit var lastMode: UserPreferences.AltimeterMode
    private val intervalometer = Intervalometer(this::updateAltitude)

    private var seaLevelPressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.altimeter_calibration, rootKey)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())

        gps = GPS(requireContext())
        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()

        distanceUnits = prefs.distanceUnits

        bindPreferences()
    }

    private fun bindPreferences() {
        altitudeTxt = findPreference(getString(R.string.pref_holder_altitude))!!
        calibrationModeList = findPreference(getString(R.string.pref_altimeter_calibration_mode))!!
        elevationCorrectionSwitch = findPreference(getString(R.string.pref_altitude_offsets))!!
        altitudeOverrideEdit = findPreference(getString(R.string.pref_altitude_override))!!
        altitudeOverrideFeetEdit = findPreference(getString(R.string.pref_altitude_override_feet))!!
        altitudeOverrideGpsBtn = findPreference(getString(R.string.pref_altitude_from_gps_btn))!!
        altitudeOverrideBarometerEdit =
            findPreference(getString(R.string.pref_altitude_override_sea_level))!!

        altitudeOverrideEdit.summary = getAltitudeOverrideString()
        altitudeOverrideFeetEdit.summary = getAltitudeOverrideFeetString()
        setOverrideStates()
        altitudeOverrideBarometerEdit.isVisible = prefs.weather.hasBarometer
        if (!prefs.weather.hasBarometer) {
            calibrationModeList.setEntries(R.array.altimeter_mode_no_barometer_entries)
            calibrationModeList.setEntryValues(R.array.altimeter_mode_no_barometer_values)
        }


        calibrationModeList.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == "barometer") {
                UiUtils.alert(
                    requireContext(),
                    getString(R.string.calibration_mode_barometer_alert_title),
                    getString(R.string.calibration_mode_barometer_alert_msg),
                    R.string.dialog_ok
                )
            }
            true
        }

        altitudeOverrideBarometerEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }
        altitudeOverrideEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }
        altitudeOverrideFeetEdit.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
        }

        altitudeOverrideGpsBtn.setOnPreferenceClickListener {
            updateElevationFromGPS()
            true
        }

        elevationCorrectionSwitch.setOnPreferenceClickListener {
            restartAltimeter()
            true
        }

        if (distanceUnits == UserPreferences.DistanceUnits.Feet) {
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

        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
            updateElevationFromBarometer(prefs.seaLevelPressureOverride)
        }

        lastMode = prefs.altimeterMode
    }

    private fun setOverrideStates() {
        val mode = prefs.altimeterMode
        val enabled =
            mode == UserPreferences.AltimeterMode.Barometer || mode == UserPreferences.AltimeterMode.Override

        altitudeOverrideFeetEdit.isEnabled = enabled
        altitudeOverrideEdit.isEnabled = enabled
        altitudeOverrideGpsBtn.isEnabled = enabled
        altitudeOverrideBarometerEdit.isEnabled = enabled
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
        ).roundPlaces(1)
            .toString() + if (distanceUnits == UserPreferences.DistanceUnits.Feet) " ft" else " m"
    }

    private fun restartAltimeter() {
        stopAltimeter()
        altimeter = sensorService.getAltimeter()
        startAltimeter()
        updateAltitude()
    }

    override fun onResume() {
        super.onResume()
        startAltimeter()
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        barometer.stop(this::onElevationFromBarometerCallback)
        barometer.stop(this::onSeaLevelPressureOverrideCallback)
        gps.stop(this::onElevationFromGPSCallback)
        stopAltimeter()
        intervalometer.stop()
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
        updateSeaLevelPressureOverride()
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

    private fun updateSeaLevelPressureOverride() {
        barometer.start(this::onSeaLevelPressureOverrideCallback)
    }

    private fun onSeaLevelPressureOverrideCallback(): Boolean {
        val altitude = prefs.altitudeOverride
        val seaLevel = WeatherService(0f, 0f, 0f).convertToSeaLevel(
            listOf(
                PressureAltitudeReading(
                    Instant.now(),
                    barometer.pressure,
                    altitude,
                    16f
                )
            )
        ).first()
        prefs.seaLevelPressureOverride = seaLevel.value
        return prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer
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
        updateSeaLevelPressureOverride()
        updateAltitude()
        UiUtils.shortToast(requireContext(), getString(R.string.altitude_override_updated_toast))
        return false
    }

    private fun updateAltitude(): Boolean {

        if (throttle.isThrottled()) {
            return true
        }

        altitudeTxt.summary = getAltitudeString()

        if (lastMode != prefs.altimeterMode) {
            lastMode = prefs.altimeterMode
            restartAltimeter()
            setOverrideStates()
            if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
                updateSeaLevelPressureOverride()
            }
        }

        altitudeOverrideFeetEdit.summary = getAltitudeOverrideFeetString()
        altitudeOverrideEdit.summary = getAltitudeOverrideString()

        return true
    }


}