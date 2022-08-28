package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTemperatureEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.UnitInputView
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.math.roundToInt

class TemperatureEstimationFragment : BoundFragment<FragmentTemperatureEstimationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val baseUnits by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService(requireContext()) }

    private val intervalometer = Timer {
        if (!isBound) {
            return@Timer
        }
        val temp = getEstimation()
        binding.temperatureTitle.title.text = if (temp == null) {
            getString(R.string.dash)
        } else {
            formatService.formatTemperature(temp.convertTo(temperatureUnits))
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTemperatureEstimationBinding {
        return FragmentTemperatureEstimationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val distanceUnits = formatService.sortDistanceUnits(
            listOf(
                DistanceUnits.Feet,
                DistanceUnits.Yards,
                DistanceUnits.Miles,
                DistanceUnits.NauticalMiles,
                DistanceUnits.Meters,
                DistanceUnits.Kilometers
            )
        )

        val temps = if (temperatureUnits == TemperatureUnits.C) {
            listOf(TemperatureUnits.C, TemperatureUnits.F)
        } else {
            listOf(TemperatureUnits.F, TemperatureUnits.C)
        }.map {
            UnitInputView.DisplayUnit(
                it,
                formatService.getTemperatureUnitName(it, true),
                formatService.getTemperatureUnitName(it, false)
            )
        }

        binding.tempEstBaseElevation.units = distanceUnits
        binding.tempEstBaseElevation.unit = baseUnits
        binding.tempEstBaseElevation.hint = getString(R.string.base_elevation)

        binding.tempEstDestElevation.units = distanceUnits
        binding.tempEstDestElevation.unit = baseUnits
        binding.tempEstDestElevation.hint = getString(R.string.destination_elevation)

        binding.tempEstBaseTemperature.units = temps
        binding.tempEstBaseTemperature.unit = temperatureUnits
        binding.tempEstBaseTemperature.hint = getString(R.string.base_temperature)

        binding.tempEstAutofill.setOnClickListener {
            autofill()
        }

        setFieldsFromSensors()
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun autofill() {
        inBackground {
            withContext(Dispatchers.Main) {
                binding.tempEstAutofill.isVisible = false
                binding.tempEstLoading.isVisible = true
                binding.tempEstBaseTemperature.isEnabled = false
                binding.tempEstBaseElevation.isEnabled = false
            }
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                    val jobs = mutableListOf<Job>()
                    jobs.add(launch { altimeter.read() })
                    jobs.add(launch { thermometer.read() })
                    jobs.joinAll()
                }
            }

            withContext(Dispatchers.Main) {
                setFieldsFromSensors()
                binding.tempEstAutofill.isVisible = true
                binding.tempEstLoading.isVisible = false
                binding.tempEstBaseTemperature.isEnabled = true
                binding.tempEstBaseElevation.isEnabled = true
            }
        }
    }

    private fun setFieldsFromSensors() {
        if (altimeter.hasValidReading) {
            val altitude = Distance.meters(altimeter.altitude).convertTo(baseUnits)
            binding.tempEstBaseElevation.value = altitude
        }

        if (thermometer.hasValidReading) {
            tryOrNothing {
                val temp = Temperature(
                    getCalibratedReading(thermometer.temperature),
                    TemperatureUnits.C
                ).convertTo(
                    temperatureUnits
                )
                binding.tempEstBaseTemperature.amount = temp.temperature.roundToInt()
                binding.tempEstBaseTemperature.unit = temperatureUnits
            }
        }
    }

    private fun getEstimation(): Temperature? {
        val baseTemp = getBaseTemperature()
        val baseElevation = getBaseElevation()
        val destElevation = getDestElevation()

        if (baseTemp == null || baseElevation == null || destElevation == null) {
            return null
        }

        return Meteorology.getTemperatureAtElevation(baseTemp, baseElevation, destElevation)
    }

    private fun getBaseTemperature(): Temperature? {
        val amount = binding.tempEstBaseTemperature.amount?.toFloat() ?: return null
        val units = binding.tempEstBaseTemperature.unit as TemperatureUnits
        val uiTemp = Temperature(amount, units)
        return uiTemp.convertTo(TemperatureUnits.C)
    }

    private fun getBaseElevation(): Distance? {
        val distance = binding.tempEstBaseElevation.value ?: return null
        return distance.meters()
    }

    private fun getDestElevation(): Distance? {
        val distance = binding.tempEstDestElevation.value ?: return null
        return distance.meters()
    }

    // TODO: Extract this
    private fun getCalibratedReading(temp: Float): Float {
        val calibrated1 = prefs.weather.minActualTemperature
        val uncalibrated1 = prefs.weather.minBatteryTemperature
        val calibrated2 = prefs.weather.maxActualTemperature
        val uncalibrated2 = prefs.weather.maxBatteryTemperature

        return calibrated1 + (calibrated2 - calibrated1) * (uncalibrated1 - temp) / (uncalibrated1 - uncalibrated2)
    }

}