package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.math.MathExtensions.toFloatCompat2
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.Instant

class CalibrateBarometerFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private var pressureTxt: Preference? = null
    private lateinit var temperatureTxt: Preference
    private var seaLevelSwitch: SwitchPreferenceCompat? = null
    private var altitudeChangeSeekBar: SeekBarPreference? = null
    private var pressureChangeSeekBar: SeekBarPreference? = null
    private lateinit var minTempCalibratedC: EditTextPreference
    private lateinit var maxTempCalibratedC: EditTextPreference
    private lateinit var minTempUncalibratedC: EditTextPreference
    private lateinit var maxTempUncalibratedC: EditTextPreference
    private lateinit var minTempCalibratedF: EditTextPreference
    private lateinit var maxTempCalibratedF: EditTextPreference
    private lateinit var minTempUncalibratedF: EditTextPreference
    private lateinit var maxTempUncalibratedF: EditTextPreference

    private val sensorChecker by lazy { SensorChecker(requireContext()) }
    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var thermometer: IThermometer

    private lateinit var weatherService: WeatherService
    private lateinit var units: PressureUnits
    private val formatService by lazy { FormatService(requireContext()) }
    private val unitService = UnitService()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.barometer_calibration, rootKey)

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())
        units = prefs.pressureUnits

        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()
        thermometer = sensorService.getThermometer()

        bindPreferences()
    }

    private fun refreshWeatherService() {
        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }

    private fun bindPreferences() {

        if (!sensorChecker.hasBarometer()){
            findPreference<Preference>(getString(R.string.pref_category_barometer_and_thermometer))?.title =
                getString(R.string.tool_thermometer_title)
            listOf(
                R.string.pref_holder_pressure,
                R.string.pref_sea_level_pressure_change_thresh,
                R.string.pref_sea_level_require_dwell,
                R.string.pref_barometer_altitude_change,
                R.string.pref_adjust_for_temperature,
                R.string.pref_sea_level_use_rapid,
                R.string.pref_pressure_history,
                R.string.pref_use_sea_level_pressure
            ).forEach {
                preferenceScreen.removePreferenceRecursively(getString(it))
            }

        }

        pressureTxt = findPreference(getString(R.string.pref_holder_pressure))
        seaLevelSwitch = findPreference(getString(R.string.pref_use_sea_level_pressure))
        temperatureTxt = findPreference(getString(R.string.pref_temperature_holder))!!
        minTempCalibratedC = findPreference(getString(R.string.pref_min_calibrated_temp_c))!!
        maxTempCalibratedC = findPreference(getString(R.string.pref_max_calibrated_temp_c))!!
        minTempUncalibratedC = findPreference(getString(R.string.pref_min_uncalibrated_temp_c))!!
        maxTempUncalibratedC = findPreference(getString(R.string.pref_max_uncalibrated_temp_c))!!
        minTempCalibratedF = findPreference(getString(R.string.pref_min_calibrated_temp_f))!!
        maxTempCalibratedF = findPreference(getString(R.string.pref_max_calibrated_temp_f))!!
        minTempUncalibratedF = findPreference(getString(R.string.pref_min_uncalibrated_temp_f))!!
        maxTempUncalibratedF = findPreference(getString(R.string.pref_max_uncalibrated_temp_f))!!
        altitudeChangeSeekBar = findPreference(getString(R.string.pref_barometer_altitude_change))
        pressureChangeSeekBar =
            findPreference(getString(R.string.pref_sea_level_pressure_change_thresh))

        altitudeChangeSeekBar?.summary =
            (if (prefs.weather.maxNonTravellingAltitudeChange == 0f) "" else "± ") + formatService.formatSmallDistance(
                prefs.weather.maxNonTravellingAltitudeChange
            )

        pressureChangeSeekBar?.summary =
            (if (prefs.weather.maxNonTravellingPressureChange == 0f) "" else "± ") + getString(
                R.string.pressure_tendency_format_2, formatService.formatPressure(
                    unitService.convert(
                        prefs.weather.maxNonTravellingPressureChange,
                        PressureUnits.Hpa,
                        prefs.pressureUnits
                    ), prefs.pressureUnits
                )
            )

        if (prefs.temperatureUnits == TemperatureUnits.C) {
            minTempCalibratedF.isVisible = false
            minTempCalibratedF.isEnabled = false
            maxTempCalibratedF.isVisible = false
            maxTempCalibratedF.isEnabled = false
            minTempUncalibratedF.isVisible = false
            minTempUncalibratedF.isEnabled = false
            maxTempUncalibratedF.isVisible = false
            maxTempUncalibratedF.isEnabled = false
            minTempCalibratedC.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.minActualTemperatureF =
                    unitService.convert(temp, TemperatureUnits.C, TemperatureUnits.F)
                update()
                true
            }
            minTempUncalibratedC.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.minBatteryTemperatureF =
                    unitService.convert(temp, TemperatureUnits.C, TemperatureUnits.F)
                update()
                true
            }
            maxTempCalibratedC.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.maxActualTemperatureF =
                    unitService.convert(temp, TemperatureUnits.C, TemperatureUnits.F)
                update()
                true
            }
            maxTempUncalibratedC.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.maxBatteryTemperatureF =
                    unitService.convert(temp, TemperatureUnits.C, TemperatureUnits.F)
                update()
                true
            }
        } else {
            minTempCalibratedC.isVisible = false
            minTempCalibratedC.isEnabled = false
            maxTempCalibratedC.isVisible = false
            maxTempCalibratedC.isEnabled = false
            minTempUncalibratedC.isVisible = false
            minTempUncalibratedC.isEnabled = false
            maxTempUncalibratedC.isVisible = false
            maxTempUncalibratedC.isEnabled = false
            minTempCalibratedF.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.minActualTemperature =
                    unitService.convert(temp, TemperatureUnits.F, TemperatureUnits.C)
                update()
                true
            }
            minTempUncalibratedF.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.minBatteryTemperature =
                    unitService.convert(temp, TemperatureUnits.F, TemperatureUnits.C)
                update()
                true
            }
            maxTempCalibratedF.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.maxActualTemperature =
                    unitService.convert(temp, TemperatureUnits.F, TemperatureUnits.C)
                update()
                true
            }
            maxTempUncalibratedF.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as String).toFloatCompat2() ?: 0f
                prefs.weather.maxBatteryTemperature =
                    unitService.convert(temp, TemperatureUnits.F, TemperatureUnits.C)
                update()
                true
            }
        }

        seaLevelSwitch?.setOnPreferenceClickListener {
            if (!altimeter.hasValidReading) {
                altimeter.start(this::updateAltitude)
            }
            refreshWeatherService()
            true
        }

        altitudeChangeSeekBar?.updatesContinuously = true
        altitudeChangeSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            altitudeChangeSeekBar?.summary =
                (if (newValue.toString()
                        .toFloat() == 0f
                ) "" else "± ") + formatService.formatSmallDistance(
                    newValue.toString().toFloat()
                )
            true
        }

        pressureChangeSeekBar?.updatesContinuously = true
        pressureChangeSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 20 * newValue.toString().toFloat() / 200f
            pressureChangeSeekBar?.summary =
                (if (change == 0f) "" else "± ") + getString(
                    R.string.pressure_tendency_format_2, formatService.formatPressure(
                        unitService.convert(
                            change,
                            PressureUnits.Hpa,
                            prefs.pressureUnits
                        ), prefs.pressureUnits
                    )
                )
            true
        }

    }


    override fun onResume() {
        super.onResume()
        refreshWeatherService()
        startBarometer()
        thermometer.start(this::updateTemperature)
        if (prefs.weather.useSeaLevelPressure && !altimeter.hasValidReading) {
            altimeter.start(this::updateAltitude)
        }
    }

    override fun onPause() {
        super.onPause()
        stopBarometer()
        altimeter.stop(this::updateAltitude)
        thermometer.stop(this::updateTemperature)
    }

    private fun updateAltitude(): Boolean {
        update()
        return false
    }

    private fun updateTemperature(): Boolean {
        update()
        return true
    }

    private fun startBarometer() {
        barometer.start(this::onPressureUpdate)
    }

    private fun stopBarometer() {
        barometer.stop(this::onPressureUpdate)
    }


    private fun onPressureUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {

        if (throttle.isThrottled()) {
            return
        }

        val seaLevelPressure = prefs.weather.useSeaLevelPressure

        val pressure = if (seaLevelPressure) {
            PressureAltitudeReading(
                Instant.now(),
                barometer.pressure,
                altimeter.altitude,
                thermometer.temperature
            ).seaLevel(prefs.weather.seaLevelFactorInTemp).value
        } else {
            barometer.pressure
        }

        pressureTxt?.summary =
            formatService.formatPressure(PressureUnitUtils.convert(pressure, units), units)
        temperatureTxt.summary =
            formatService.formatTemperature(thermometer.temperature, prefs.temperatureUnits)
    }


}