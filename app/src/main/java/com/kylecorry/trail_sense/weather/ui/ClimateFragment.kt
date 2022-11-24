package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.loading.AlertLoadingIndicator
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
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

class ClimateFragment : BoundFragment<FragmentClimateBinding>() {

    private val weather by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private val location by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val units by lazy { UserPreferences(requireContext()).temperatureUnits }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val loading by lazy { AlertLoadingIndicator(requireContext(), "Loading climate data") }

    private var currentYear = 0
    private var temperatures: List<Pair<Int, Range<Temperature>>> = emptyList()

    private val chart by lazy {
        YearlyTemperatureRangeChart(binding.temperatureChart) {
            binding.displayDate.date = LocalDate.ofYearDay(binding.displayDate.date.year, it)
        }
    }

    // TODO: Allow location / elevation to be changed

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadTemperatures()

        binding.displayDate.setOnDateChangeListener {
            reloadTemperatures()
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentClimateBinding {
        return FragmentClimateBinding.inflate(layoutInflater, container, false)
    }

    private fun reloadTemperatures() {
        loadTemperatures(binding.displayDate.date, location.location, location.elevation)
    }

    private fun loadTemperatures(date: LocalDate, location: Coordinate, elevation: Distance) {
        inBackground {
            onMain { loading.show() }
            if (date.year != currentYear) {
                temperatures = onDefault {
                    (1..date.lengthOfYear()).map {
                        it to weather.getTemperatureRange(
                            date.withDayOfYear(it),
                            location,
                            elevation
                        )
                    }
                }
                currentYear = date.year
            }

            val range = temperatures[date.dayOfYear - 1].second

            onMain {
                plotTemperatures(temperatures, date.year)
                updateTitle(range)
                loading.hide()
            }
        }
    }

    private fun updateTitle(range: Range<Temperature>) {
        val lowValue = formatter.formatTemperature(
            range.start.convertTo(units)
        )
        val highValue = formatter.formatTemperature(
            range.end.convertTo(units)
        )
        binding.climateTitle.title.text =
            getString(R.string.slash_separated_pair, highValue, lowValue)
    }

    private fun plotTemperatures(data: List<Pair<Int, Range<Temperature>>>, year: Int) {
        chart.plot(data, units, year)
    }

}