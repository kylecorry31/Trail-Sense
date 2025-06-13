package com.kylecorry.trail_sense.tools.climate.ui

import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateGroup
import com.kylecorry.sol.science.meteorology.KoppenGeigerSeasonalPrecipitationPattern
import com.kylecorry.sol.science.meteorology.KoppenGeigerTemperaturePattern
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.DatePickerView
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.tools.climate.domain.BiologicalActivity
import com.kylecorry.trail_sense.tools.climate.domain.BiologicalActivityType
import com.kylecorry.trail_sense.tools.climate.domain.PhenologyService
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.LocalDate
import java.time.Month

class ClimateFragment : TrailSenseReactiveFragment(R.layout.fragment_climate) {

    override fun update() {
        // Views
        val locationInput = useView<CoordinateInputView>(R.id.location)
        val elevationInput = useView<ElevationInputView>(R.id.elevation)
        val datePickerInput = useView<DatePickerView>(R.id.display_date)
        val titleView = useView<Toolbar>(R.id.climate_title)
        val temperatureChartView = useView<Chart>(R.id.temperature_chart)
        val precipitationChartView = useView<Chart>(R.id.precipitation_chart)
        val climateZoneTitleView = useView<TextView>(R.id.climate_zone_title)
        val climateZoneDescriptionView = useView<TextView>(R.id.climate_zone_description)
        val temperatureTitleView = useView<TextView>(R.id.temperature_title)
        val precipitationTitleView = useView<TextView>(R.id.precipitation_title)
        val insectActivityTitleView = useView<TextView>(R.id.insect_activity)
        val insectActivityDescriptionView = useView<TextView>(R.id.insect_activity_description)

        // Services
        val locationSubsystem = useService<LocationSubsystem>()
        val weatherSubsystem = useService<WeatherSubsystem>()
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()

        // Prefs
        val temperatureUnits = useMemo(prefs) {
            prefs.temperatureUnits
        }
        val distanceUnits = useMemo(prefs) {
            prefs.baseDistanceUnits
        }
        val precipitationDistanceUnits = useMemo(distanceUnits) {
            if (distanceUnits.isMetric) {
                DistanceUnits.Millimeters
            } else {
                DistanceUnits.Inches
            }
        }
        val isInsectActivityEnabled = useMemo(prefs) {
            prefs.climate.isInsectActivityEnabled
        }

        // State
        val (date, setDate) = useState(LocalDate.now())
        val (location, setLocation) = useState(locationSubsystem.location)
        val (elevation, setElevation) = useState(locationSubsystem.elevation.convertTo(distanceUnits))

        // Charts
        val temperatureChart = useMemo(temperatureChartView) {
            YearlyTemperatureRangeChart(temperatureChartView) {
                setDate(it)
            }
        }

        val precipitationChart = useMemo(precipitationChartView) {
            MonthlyPrecipitationChart(precipitationChartView) {
                // TODO: This really needs the prev version of a setter
                setDate(LocalDate.now().withDayOfMonth(15).withMonth(it.value))
            }
        }

        val temperatures = useBackgroundMemo(
            date.year,
            location,
            elevation,
            cancelWhenRerun = true
        ) {
            weatherSubsystem.getTemperatureRanges(
                date.year,
                location,
                elevation,
                calibrated = false
            )
        }

        val precipitation = useBackgroundMemo(
            date.year,
            location,
            cancelWhenRerun = true
        ) {
            weatherSubsystem.getMonthlyPrecipitation(location)
        }

        val climateClassification = useMemo(
            temperatures,
            precipitation
        ) {
            if (temperatures == null || precipitation == null) {
                return@useMemo null
            }
            getClimateDescription(temperatures, precipitation)
        }

        val activityPatterns =
            useBackgroundMemo(date.year, location, elevation, cancelWhenRerun = true) {
                PhenologyService(weatherSubsystem).getYearlyActiveDays(
                    date.year, location, elevation, calibrated = false
                )
            }

        // View effects
        useEffectWithCleanup(
            lifecycleHookTrigger.onResume(),
            locationInput,
            elevationInput,
            datePickerInput,
            titleView,
            locationSubsystem
        ) {

            titleView.subtitle.text = getString(R.string.historic_temperature_years, 30)

            // Set initial values
            val roundedElevation = elevation.copy(
                distance = elevation.distance.roundPlaces(
                    Units.getDecimalPlaces(distanceUnits)
                )
            )
            elevationInput.elevation = roundedElevation
            locationInput.coordinate = location
            datePickerInput.date = LocalDate.now()

            // On change listeners
            locationInput.setOnCoordinateChangeListener {
                setLocation(it ?: locationSubsystem.location)
            }

            elevationInput.setOnElevationChangeListener {
                setElevation(it ?: locationSubsystem.elevation)
            }

            locationInput.setOnAutoLocationClickListener {
                if (elevationInput.elevation == null) {
                    elevationInput.autofill()
                }
            }

            elevationInput.setOnAutoElevationClickListener {
                if (locationInput.coordinate == null) {
                    locationInput.autofill()
                }
            }

            locationInput.setOnBeaconSelectedListener {
                it.elevation?.let { elevation ->
                    elevationInput.elevation = Distance.meters(elevation).convertTo(distanceUnits)
                }
            }

            datePickerInput.setOnDateChangeListener {
                setDate(it)
            }

            return@useEffectWithCleanup {
                locationInput.pause()
                elevationInput.pause()
            }
        }

        useEffect(date, datePickerInput) {
            datePickerInput.date = date
        }

        // Temperature
        useEffect(temperatures, temperatureTitleView, date) {
            val range = temperatures?.firstOrNull { it.first == date }?.second ?: return@useEffect
            val lowValue = formatter.formatTemperature(
                range.start.convertTo(temperatureUnits)
            )
            val highValue = formatter.formatTemperature(
                range.end.convertTo(temperatureUnits)
            )
            temperatureTitleView.text = getString(
                R.string.climate_temperature_amount,
                getString(R.string.slash_separated_pair, highValue, lowValue)
            )
        }

        useEffect(temperatures, temperatureChart, date) {
            temperatures ?: return@useEffect
            temperatureChart.plot(temperatures, temperatureUnits)
            temperatureChart.highlight(date)
        }

        // Precipitation
        useEffect(precipitation, precipitationTitleView, date) {
            val value = precipitation?.get(date.month)?.convertTo(precipitationDistanceUnits)
                ?: return@useEffect
            val formattedValue = formatter.formatDistance(
                value, if (precipitationDistanceUnits.isMetric) {
                    0
                } else {
                    1
                }
            )
            precipitationTitleView.text =
                getString(R.string.climate_precipitation_amount, formattedValue)
        }

        useEffect(precipitation, precipitationChart, date.month) {
            precipitation ?: return@useEffect
            precipitationChart.plot(precipitation, precipitationDistanceUnits)
            precipitationChart.highlight(date.month)
        }

        // Climate zone
        useEffect(climateClassification, climateZoneTitleView, climateZoneDescriptionView) {
            climateClassification?.let {
                climateZoneTitleView.text = it.first
                climateZoneDescriptionView.text = it.second
            }
        }

        // Activity
        useEffect(activityPatterns, insectActivityDescriptionView, formatter) {
            val insects = activityPatterns?.entries
                ?.filter { it.key.type == BiologicalActivityType.Insect && it.value.isNotEmpty() }
                ?: emptyList()

            insectActivityDescriptionView.text = insects
                .joinToString("\n") {
                    "${getBiologicalActivityName(it.key)}: ${formatActivity(formatter, it.value)}"
                }

            insectActivityDescriptionView.isVisible =
                isInsectActivityEnabled && insects.isNotEmpty()
            insectActivityTitleView.isVisible = isInsectActivityEnabled && insects.isNotEmpty()
        }
    }

    override fun onPause() {
        super.onPause()
        cleanupEffects()
    }

    private fun isActivityWrapped(activePeriods: List<Range<LocalDate>>): Boolean {
        if (activePeriods.size != 2) {
            return false
        }

        val first = activePeriods[0]
        val second = activePeriods[1]

        // If the first is on Jan 1 and the second ends on Dec 31, then it's wrapped
        return first.start.dayOfYear == 1 && second.end.month == Month.DECEMBER && second.end.dayOfMonth == 31
    }

    private fun getBiologicalActivityName(activity: BiologicalActivity): String {
        return when (activity) {
            BiologicalActivity.Mosquito -> getString(R.string.mosquitoes)
            BiologicalActivity.Tick -> getString(R.string.ticks)
        }
    }

    private fun formatActivity(formatter: FormatService, activity: List<Range<LocalDate>>): String {
        val start = if (isActivityWrapped(activity)) {
            activity[1].start.month
        } else {
            activity[0].start.month
        }

        val end = if (isActivityWrapped(activity)) {
            activity[0].end.month
        } else {
            activity[0].end.month
        }

        return getString(
            R.string.active_period, formatter.formatMonth(start), formatter.formatMonth(
                end
            )
        )
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