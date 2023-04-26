package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTemperatureEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.readAll
import com.kylecorry.trail_sense.shared.views.UnitInputView
import java.time.Duration
import kotlin.math.roundToInt

class TemperatureEstimationFragment : BoundFragment<FragmentTemperatureEstimationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val location by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private val intervalometer = Timer {
        if (!isBound) {
            return@Timer
        }
        val temp = getEstimation()
        binding.temperatureTitle.title.text = if (temp == null || temp.temperature.isNaN()) {
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

        binding.tempEstBaseElevation.hint = getString(R.string.base_elevation)
        binding.tempEstDestElevation.hint = getString(R.string.destination_elevation)

        binding.tempEstBaseTemperature.units = temps
        binding.tempEstBaseTemperature.unit = temperatureUnits
        binding.tempEstBaseTemperature.hint = getString(R.string.base_temperature)

        binding.tempEstAutofill.setOnClickListener {
            autofill()
        }

        setThermometerFieldFromSensor()

        // Set the initial elevation
        val elevation = location.elevation.convertTo(prefs.baseDistanceUnits)
        val roundedElevation = elevation.copy(
            distance = elevation.distance.roundPlaces(
                Units.getDecimalPlaces(elevation.units)
            )
        )
        binding.tempEstBaseElevation.elevation = roundedElevation
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
        binding.tempEstBaseElevation.pause()
        binding.tempEstDestElevation.pause()
    }

    private fun autofill() {
        inBackground {
            onMain {
                binding.tempEstAutofill.isVisible = false
                binding.tempEstLoading.isVisible = true
                binding.tempEstBaseTemperature.isEnabled = false
                binding.tempEstBaseElevation.autofill()
            }

            readAll(
                listOf(thermometer),
                Duration.ofSeconds(10),
                forceStopOnCompletion = true
            )

            onMain {
                setThermometerFieldFromSensor()
                binding.tempEstAutofill.isVisible = true
                binding.tempEstLoading.isVisible = false
                binding.tempEstBaseTemperature.isEnabled = true
            }
        }
    }

    private fun setThermometerFieldFromSensor() {
        val sensorTemperature = thermometer.temperature

        if ((thermometer.hasValidReading || sensorTemperature != 0f) && !sensorTemperature.isNaN()) {
            val temp = Temperature.celsius(sensorTemperature).convertTo(temperatureUnits)
            binding.tempEstBaseTemperature.amount = temp.temperature.roundToInt()
            binding.tempEstBaseTemperature.unit = temperatureUnits
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
        val distance = binding.tempEstBaseElevation.elevation ?: return null
        return distance.meters()
    }

    private fun getDestElevation(): Distance? {
        val distance = binding.tempEstDestElevation.elevation ?: return null
        return distance.meters()
    }

}