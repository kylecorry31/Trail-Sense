package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.sensors.temperature.IThermometer
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.domain.PressureUnits
import com.kylecorry.trail_sense.weather.domain.WeatherService
import java.time.Instant

class CalibrateBarometerFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)

    private lateinit var pressureTxt: Preference
    private lateinit var seaLevelSwitch: SwitchPreferenceCompat

    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var thermometer: IThermometer

    private lateinit var weatherService: WeatherService
    private lateinit var units: PressureUnits

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

        val symbol = PressureUnitUtils.getSymbol(units)
        val format = PressureUnitUtils.getDecimalFormat(units)
        pressureTxt.summary =
            "${format.format(PressureUnitUtils.convert(pressure, units))}  $symbol"
    }


}