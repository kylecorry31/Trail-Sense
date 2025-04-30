package com.kylecorry.trail_sense.tools.climate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateGroup
import com.kylecorry.sol.science.meteorology.KoppenGeigerSeasonalPrecipitationPattern
import com.kylecorry.sol.science.meteorology.KoppenGeigerTemperaturePattern
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentClimateBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.LocalDate
import java.time.Month

class ClimateFragment : BoundFragment<FragmentClimateBinding>() {

    private val weather by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private val location by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val distanceUnits by lazy { prefs.baseDistanceUnits }
    private val precipitationDistanceUnits by lazy {
        if (distanceUnits.isMetric) {
            DistanceUnits.Millimeters
        } else {
            DistanceUnits.Inches
        }
    }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private var temperatures: List<Pair<LocalDate, Range<Temperature>>> = emptyList()
    private var precipitation: Map<Month, Distance> = emptyMap()
    private var currentYear = 0
    private var climateName: String = ""
    private var climateDescription: String = ""

    private val temperatureChart by lazy {
        YearlyTemperatureRangeChart(binding.temperatureChart) {
            binding.displayDate.date = it
        }
    }

    private val precipitationChart by lazy {
        MonthlyPrecipitationChart(binding.precipitationChart) {
            binding.displayDate.date = binding.displayDate.date.withMonth(it.value)
        }
    }

    private val runner = CoroutineQueueRunner()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.climateTitle.subtitle.text = getString(R.string.historic_temperature_years, 30)

        // Populate the initial location and elevation
        binding.location.coordinate = location.location
        val elevation = location.elevation.convertTo(distanceUnits)
        val roundedElevation = elevation.copy(
            distance = elevation.distance.roundPlaces(
                Units.getDecimalPlaces(distanceUnits)
            )
        )
        binding.elevation.elevation = roundedElevation

        reloadTemperatures()

        // TODO: Make this a dialog
        binding.location.setOnCoordinateChangeListener {
            reloadTemperatures()
        }

        binding.location.setOnBeaconSelectedListener {
            it.elevation?.let { elevation ->
                binding.elevation.elevation = Distance.meters(elevation).convertTo(distanceUnits)
            }
            reloadTemperatures()
        }

        binding.location.setOnAutoLocationClickListener {
            if (binding.elevation.elevation == null) {
                binding.elevation.autofill()
            }
        }

        binding.elevation.setOnAutoElevationClickListener {
            if (binding.location.coordinate == null) {
                binding.location.autofill()
            }
        }

        binding.elevation.setOnElevationChangeListener {
            reloadTemperatures()
        }

        binding.displayDate.setOnDateChangeListener {
            reloadTemperatures(recalculate = it.year != currentYear)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.location.pause()
        binding.elevation.pause()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentClimateBinding {
        return FragmentClimateBinding.inflate(layoutInflater, container, false)
    }

    private fun reloadTemperatures(recalculate: Boolean = true) {
        loadData(
            binding.displayDate.date,
            binding.location.coordinate ?: location.location,
            binding.elevation.elevation ?: Distance.meters(0f),
            recalculate
        )
    }

    private fun loadData(
        date: LocalDate,
        location: Coordinate,
        elevation: Distance,
        recalculate: Boolean
    ) {
        inBackground {
            runner.replace {
                if (recalculate) {
                    temperatures = weather.getTemperatureRanges(
                        date.year,
                        location,
                        elevation,
                        calibrated = false
                    )
                    precipitation = weather.getMonthlyPrecipitation(location)
                    currentYear = date.year
                    val climate = getClimateDescription(temperatures, precipitation)
                    climateName = climate.first
                    climateDescription = climate.second
                }

                val range = temperatures.first { it.first == date }.second

                onMain {
                    if (isBound) {
                        plotTemperatures(temperatures)
                        plotPrecipitation(precipitation)
                        updateTemperatureTitle(range)
                        updatePrecipitationTitle(
                            precipitation[binding.displayDate.date.month] ?: Distance.meters(0f)
                        )
                        updateClimateZoneDetails()
                    }
                }
            }
        }
    }

    private fun updateTemperatureTitle(range: Range<Temperature>) {
        val lowValue = formatter.formatTemperature(
            range.start.convertTo(temperatureUnits)
        )
        val highValue = formatter.formatTemperature(
            range.end.convertTo(temperatureUnits)
        )
        binding.temperatureTitle.text = getString(
            R.string.climate_temperature_amount,
            getString(R.string.slash_separated_pair, highValue, lowValue)
        )
    }

    private fun updatePrecipitationTitle(precipitation: Distance) {
        val value = precipitation.convertTo(precipitationDistanceUnits)
        val formattedValue = formatter.formatDistance(
            value, if (precipitationDistanceUnits.isMetric) {
                0
            } else {
                1
            }
        )
        binding.precipitationTitle.text =
            getString(R.string.climate_precipitation_amount, formattedValue)
    }

    private fun plotTemperatures(data: List<Pair<LocalDate, Range<Temperature>>>) {
        temperatureChart.plot(data, temperatureUnits)
        temperatureChart.highlight(binding.displayDate.date)
    }

    private fun plotPrecipitation(data: Map<Month, Distance>) {
        precipitationChart.plot(data, precipitationDistanceUnits)
        precipitationChart.highlight(binding.displayDate.date.month)
    }

    private fun updateClimateZoneDetails() {
        binding.climateZoneTitle.text = climateName
        binding.climateZoneDescription.text = climateDescription
    }

    private fun getClimateDescription(
        temperatures: List<Pair<LocalDate, Range<Temperature>>>,
        precipitation: Map<Month, Distance>
    ): Pair<String, String> {

        // TODO: List out dangers

        val monthlyAverageTemperatures = temperatures
            .filter { it.first.dayOfMonth == 15 }
            .map { it.first.month to Temperature.celsius((it.second.start.celsius().temperature + it.second.end.celsius().temperature) / 2) }
            .toMap()

        val climate = Meteorology.getKoppenGeigerClimateClassification(
            monthlyAverageTemperatures,
            precipitation
        )

        val sentences = mutableListOf<String>()
        sentences.add(
            when (climate.climateGroup) {
                KoppenGeigerClimateGroup.Tropical -> getString(R.string.climate_tropical_description)
                KoppenGeigerClimateGroup.Dry -> getString(R.string.climate_dry_description)
                KoppenGeigerClimateGroup.Temperate -> getString(R.string.climate_temperate_description)
                KoppenGeigerClimateGroup.Continental -> getString(R.string.climate_continental_description)
                KoppenGeigerClimateGroup.Polar -> getString(R.string.climate_polar_description)
            }
        )
        sentences.add(
            when (climate.seasonalPrecipitationPattern) {
                KoppenGeigerSeasonalPrecipitationPattern.Rainforest -> getString(R.string.climate_rainforest_description)
                KoppenGeigerSeasonalPrecipitationPattern.Monsoon -> getString(R.string.climate_monsoon_description)
                KoppenGeigerSeasonalPrecipitationPattern.Savanna -> getString(R.string.climate_savanna_description)
                KoppenGeigerSeasonalPrecipitationPattern.Desert -> getString(R.string.climate_desert_description)
                KoppenGeigerSeasonalPrecipitationPattern.Steppe -> getString(R.string.climate_steppe_description)
                KoppenGeigerSeasonalPrecipitationPattern.DrySummer -> getString(R.string.climate_dry_summer_description)
                KoppenGeigerSeasonalPrecipitationPattern.DryWinter -> getString(R.string.climate_dry_winter_description)
                KoppenGeigerSeasonalPrecipitationPattern.NoDrySeason -> getString(R.string.climate_no_dry_season_description)
                KoppenGeigerSeasonalPrecipitationPattern.Tundra -> getString(R.string.climate_tundra_description)
                KoppenGeigerSeasonalPrecipitationPattern.IceCap -> getString(R.string.climate_ice_cap_description)
                null -> ""
            }
        )
        sentences.add(
            when (climate.temperaturePattern) {
                KoppenGeigerTemperaturePattern.Hot -> getString(R.string.climate_hot_description)
                KoppenGeigerTemperaturePattern.Cold -> getString(R.string.climate_cold_description)
                KoppenGeigerTemperaturePattern.HotSummer -> getString(R.string.climate_hot_summer_description)
                KoppenGeigerTemperaturePattern.WarmSummer -> getString(R.string.climate_warm_summer_description)
                KoppenGeigerTemperaturePattern.ColdSummer -> getString(R.string.climate_cold_summer_description)
                KoppenGeigerTemperaturePattern.VeryColdWinter -> getString(R.string.climate_very_cold_winter_description)
                null -> ""
            }
        )

        val name = when {
            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Monsoon -> getString(
                R.string.climate_tropical_monsoon
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Savanna -> getString(
                R.string.climate_tropical_savanna
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Rainforest -> getString(
                R.string.climate_tropical_rainforest
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Desert -> getString(
                R.string.climate_desert
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Steppe -> getString(
                R.string.climate_steppe
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Tundra -> getString(
                R.string.climate_tundra
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.IceCap -> getString(
                R.string.climate_ice_cap
            )
            // TODO: Break these down (ex. borreal, tiaga)
            climate.climateGroup == KoppenGeigerClimateGroup.Temperate -> getString(R.string.climate_temperate)
            climate.climateGroup == KoppenGeigerClimateGroup.Continental -> getString(R.string.climate_continental)
            else -> getString(R.string.unknown)
        }

        return name to sentences.filter { it.isNotEmpty() }.joinToString(" ")


    }

}