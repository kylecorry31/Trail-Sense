package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.Instant
import kotlin.math.roundToInt

class CalibrateBarometerFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private lateinit var pressureTxt: Preference
    private lateinit var temperatureTxt: Preference
    private lateinit var seaLevelSwitch: SwitchPreferenceCompat
    private lateinit var cSeekBar: SeekBarPreference
    private lateinit var fSeekBar: SeekBarPreference

    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var thermometer: IThermometer

    private lateinit var weatherService: WeatherService
    private lateinit var units: PressureUnits
    private val formatService by lazy { FormatService(requireContext()) }

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
        pressureTxt = findPreference(getString(R.string.pref_holder_pressure))!!
        seaLevelSwitch = findPreference(getString(R.string.pref_use_sea_level_pressure))!!
        temperatureTxt = findPreference(getString(R.string.pref_temperature_holder))!!
        cSeekBar = findPreference(getString(R.string.pref_temperature_adjustment_c))!!
        fSeekBar = findPreference(getString(R.string.pref_temperature_adjustment_f))!!

        if (prefs.temperatureUnits == TemperatureUnits.C) {
            fSeekBar.isVisible = false
            fSeekBar.isEnabled = false
        } else {
            cSeekBar.isVisible = false
            cSeekBar.isEnabled = false
        }

        if (fSeekBar.isEnabled) {
            fSeekBar.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as Int).toFloat()
                prefs.weather.temperatureAdjustment = (temp * 5 / 9f).roundToInt()
                update()
                true
            }
        }

        if (cSeekBar.isEnabled) {
            cSeekBar.setOnPreferenceChangeListener { _, newValue ->
                val temp = (newValue as Int).toFloat()
                preferenceManager.sharedPreferences.edit {
                    putInt(
                        getString(R.string.pref_temperature_adjustment_f),
                        (temp * 9 / 5f).roundToInt()
                    )
                }
                update()
                true
            }
        }

        seaLevelSwitch.setOnPreferenceClickListener {
            if (!altimeter.hasValidReading) {
                altimeter.start(this::updateAltitude)
            }
            refreshWeatherService()
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
            weatherService.convertToSeaLevel(
                listOf(
                    PressureAltitudeReading(
                        Instant.now(),
                        barometer.pressure,
                        altimeter.altitude,
                        thermometer.temperature
                    )
                )
            ).first().value
        } else {
            barometer.pressure
        }

        pressureTxt.summary =
            formatService.formatPressure(PressureUnitUtils.convert(pressure, units), units)
        temperatureTxt.summary =
            formatService.formatTemperature(thermometer.temperature, prefs.temperatureUnits)
    }


}