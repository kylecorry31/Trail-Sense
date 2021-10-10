package com.kylecorry.trail_sense.weather.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.math.SolMath.movingAverage
import com.kylecorry.sol.science.meteorology.HeatAlert
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentThermometerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant

class ThermometerFragment : BoundFragment<FragmentThermometerBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val weatherService by lazy {
        WeatherService(prefs.weather)
    }

    private val repo by lazy { WeatherRepo.getInstance(requireContext()) }

    private lateinit var temperatureChart: TemperatureChart

    private lateinit var units: TemperatureUnits

    private var heatAlertTitle = ""
    private var heatAlertContent = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        temperatureChart = TemperatureChart(binding.chart)

        binding.heatAlert.setOnClickListener {
            Alerts.dialog(requireContext(), heatAlertTitle, heatAlertContent, cancelText = null)
        }

        binding.freezingAlert.setOnClickListener {
            Alerts.dialog(
                requireContext(), getString(R.string.freezing_temperatures_warning), getString(
                    R.string.freezing_temperatures_description
                ), cancelText = null
            )
        }

        CustomUiUtils.setButtonState(binding.temperatureEstimationBtn, false)
        binding.temperatureEstimationBtn.setOnClickListener {
            findNavController().navigate(R.id.action_thermometer_to_temperature_estimation)
        }

        thermometer.asLiveData().observe(viewLifecycleOwner, { onTemperatureUpdate() })
        repo.getAllLive().observe(viewLifecycleOwner) {
            updateChart(
                it.map { Reading(weatherService.calibrateTemperature(it.value.temperature), it.time) }
                    .sortedBy { it.time }
                    .filter { it.time <= Instant.now() }
            )
        }

        updateUI()

    }

    override fun onResume() {
        super.onResume()
        units = prefs.temperatureUnits
    }

    private fun updateChart(readings: List<Reading<Float>>) {
        if (readings.isNotEmpty()) {
            val totalTime = Duration.between(
                readings.first().time, Instant.now()
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> binding.tempChartTitle.text =
                    getString(R.string.temperature) + " - " + context?.resources?.getQuantityString(
                        R.plurals.last_minutes,
                        minutes.toInt(),
                        minutes
                    )
                else -> {
                    if (minutes >= 30) hours++
                    binding.tempChartTitle.text =
                        getString(R.string.temperature) + " - " +
                                context?.resources?.getQuantityString(
                                    R.plurals.last_hours,
                                    hours.toInt(),
                                    hours
                                )
                }
            }

        }

        val filtered = movingAverage(readings.map { it.value }, 8).mapIndexed { index, value ->
            Reading(Temperature.celsius(value).convertTo(units).temperature, readings[index].time)
        }

        temperatureChart.plot(filtered)
    }

    private fun updateUI() {
        val hasTemp = thermometer.hasValidReading
        val uncalibrated = thermometer.temperature

        val reading = weatherService.calibrateTemperature(thermometer.temperature)

        if (!hasTemp) {
            binding.temperature.text = getString(R.string.dash)
        } else {
            binding.batteryTemp.text = getString(
                R.string.battery_temp,
                formatService.formatTemperature(
                    Temperature(uncalibrated, TemperatureUnits.C).convertTo(prefs.temperatureUnits)
                )
            )
            binding.temperature.text =
                formatService.formatTemperature(
                    Temperature(reading, TemperatureUnits.C).convertTo(
                        prefs.temperatureUnits
                    )
                )
            binding.freezingAlert.visibility = if (reading <= 0f) View.VISIBLE else View.INVISIBLE
        }

        if (hasTemp) {
            val alert =
                weatherService.getHeatAlert(
                    weatherService.getHeatIndex(
                        reading,
                        50f
                    )
                )
            showHeatAlert(alert)
        } else {
            showHeatAlert(HeatAlert.Normal)
        }

    }

    private fun showHeatAlert(alert: HeatAlert) {
        if (alert != HeatAlert.Normal) {
            binding.heatAlert.visibility = View.VISIBLE
        } else {
            binding.heatAlert.visibility = View.INVISIBLE
        }

        val alertColor = when (alert) {
            HeatAlert.FrostbiteCaution, HeatAlert.FrostbiteWarning, HeatAlert.FrostbiteDanger -> Resources.color(
                requireContext(),
                R.color.colorAccent
            )
            else -> Resources.color(requireContext(), R.color.colorPrimary)
        }

        binding.heatAlert.imageTintList = ColorStateList.valueOf(alertColor)

        heatAlertTitle = getHeatAlertTitle(alert)
        heatAlertContent = getHeatAlertMessage(alert)
    }

    private fun getHeatAlertTitle(alert: HeatAlert): String {
        return when (alert) {
            HeatAlert.HeatDanger -> getString(R.string.heat_alert_heat_danger_title)
            HeatAlert.HeatAlert -> getString(R.string.heat_alert_heat_alert_title)
            HeatAlert.HeatCaution -> getString(R.string.heat_alert_heat_caution_title)
            HeatAlert.HeatWarning -> getString(R.string.heat_alert_heat_warning_title)
            HeatAlert.FrostbiteWarning -> getString(R.string.heat_alert_frostbite_warning_title)
            HeatAlert.FrostbiteCaution -> getString(R.string.heat_alert_frostbite_caution_title)
            HeatAlert.FrostbiteDanger -> getString(R.string.heat_alert_frostbite_danger_title)
            else -> getString(R.string.heat_alert_normal_title)
        }
    }

    private fun getHeatAlertMessage(alert: HeatAlert): String {
        return when (alert) {
            HeatAlert.HeatWarning, HeatAlert.HeatCaution, HeatAlert.HeatAlert, HeatAlert.HeatDanger -> getString(
                R.string.heat_alert_heat_message
            )
            HeatAlert.FrostbiteWarning, HeatAlert.FrostbiteCaution, HeatAlert.FrostbiteDanger -> getString(
                R.string.heat_alert_frostbite_message
            )
            else -> ""
        }
    }

    private fun onTemperatureUpdate(): Boolean {
        if (thermometer.temperature == 0f) {
            return true
        }
        updateUI()
        return true
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentThermometerBinding {
        return FragmentThermometerBinding.inflate(layoutInflater, container, false)
    }
}
