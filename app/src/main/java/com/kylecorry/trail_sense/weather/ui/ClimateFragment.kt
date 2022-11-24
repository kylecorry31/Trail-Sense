package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentClimateBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.weather.ui.charts.YearlyTemperatureRangeChart
import java.time.LocalDate
import java.time.Month

class ClimateFragment : BoundFragment<FragmentClimateBinding>() {

    private val weather by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private val location by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val distanceUnits by lazy { prefs.baseDistanceUnits }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private var temperatures: List<Pair<Month, Range<Temperature>>> = emptyList()

    private val chart by lazy {
        YearlyTemperatureRangeChart(binding.temperatureChart) {
            binding.displayDate.date = LocalDate.of(binding.displayDate.date.year, it, 1)
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.location.coordinate = location.location
        updateElevationUnits()
        binding.elevation.value = location.elevation.convertTo(distanceUnits)
        binding.elevation.hint = getString(R.string.elevation)
        binding.elevation.defaultHint = getString(R.string.elevation)

        reloadTemperatures()

        // TODO: Make this a dialog
        binding.location.setOnCoordinateChangeListener {
            reloadTemperatures()
        }

        binding.elevation.setOnValueChangeListener {
            reloadTemperatures()
        }

        binding.displayDate.setOnDateChangeListener {
            reloadTemperatures(recalculate = false)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.location.pause()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentClimateBinding {
        return FragmentClimateBinding.inflate(layoutInflater, container, false)
    }

    private fun reloadTemperatures(recalculate: Boolean = true) {
        loadTemperatures(
            binding.displayDate.date,
            binding.location.coordinate ?: location.location,
            binding.elevation.value ?: location.elevation,
            recalculate
        )
    }

    private fun loadTemperatures(
        date: LocalDate,
        location: Coordinate,
        elevation: Distance,
        recalculate: Boolean
    ) {
        inBackground {
            if (recalculate) {
                temperatures = onDefault {
                    Month.values().map {
                        it to weather.getTemperatureRange(
                            LocalDate.of(date.year, it, 15),
                            location,
                            elevation
                        )
                    }
                }
            }

            val range = weather.getTemperatureRange(date, location, elevation)

            onMain {
                plotTemperatures(temperatures)
                updateTitle(range)
            }
        }
    }

    private fun updateTitle(range: Range<Temperature>) {
        val lowValue = formatter.formatTemperature(
            range.start.convertTo(temperatureUnits)
        )
        val highValue = formatter.formatTemperature(
            range.end.convertTo(temperatureUnits)
        )
        binding.climateTitle.title.text =
            getString(R.string.slash_separated_pair, highValue, lowValue)
    }

    private fun plotTemperatures(data: List<Pair<Month, Range<Temperature>>>) {
        chart.plot(data, temperatureUnits)
    }

    private fun updateElevationUnits() {
        binding.elevation.units =
            formatter.sortDistanceUnits(listOf(DistanceUnits.Meters, DistanceUnits.Feet))
    }

}