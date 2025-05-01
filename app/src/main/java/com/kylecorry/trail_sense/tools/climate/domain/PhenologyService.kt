package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateGroup
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import java.time.LocalDate
import java.time.Month

enum class BiologicalActivityType {
    Insect,
    Pollen,
    Foliage,
}

enum class BiologicalActivity(val type: BiologicalActivityType) {
    Mosquito(BiologicalActivityType.Insect)
}

class PhenologyService(private val weather: IWeatherSubsystem) {

    private val phenology = mapOf(
        // https://www.nrcc.cornell.edu/industry/mosquito/degreedays.html
        BiologicalActivity.Mosquito to SpeciesPhenology(
            Temperature.celsius(10f),
            listOf(
                LifecycleEvent(
                    EVENT_ACTIVE_START,
                    MinimumGrowingDegreeDaysTrigger(230f, TemperatureUnits.F)
                ),
                LifecycleEvent(
                    EVENT_ACTIVE_END,
                    FirstFrostTrigger(Temperature.celsius(5f))
                )
            ),
            growingDegreeDaysCalculationType = GrowingDegreeDaysCalculationType.BaseMax
        )
    )

    private val excludedClimates = mapOf(
        BiologicalActivity.Mosquito to listOf(
            KoppenGeigerClimateGroup.Dry,
            KoppenGeigerClimateGroup.Polar
        )
    )

    private fun getActivePeriodsForYear(
        year: Int,
        events: List<Pair<LocalDate, LifecycleEvent>>,
        activeStart: String,
        activeEnd: String
    ): List<Range<LocalDate>> {
        val activePeriods = mutableListOf<Range<LocalDate>>()
        var startDate: LocalDate = LocalDate.of(year, 1, 1)
        var hasStartDate = false
        for (event in events.filter { it.first.year == year }) {
            if (event.second.name == activeEnd) {
                activePeriods.add(Range(startDate, event.first))
                hasStartDate = false
            } else if (event.second.name == activeStart) {
                startDate = event.first
                hasStartDate = true
            }
        }

        if (hasStartDate) {
            activePeriods.add(Range(startDate, LocalDate.of(year, 12, 31)))
        }

        return activePeriods
    }

    // TODO: Pass in yearly temps, climate zone, etc - and only call from weather subsystem
    suspend fun getYearlyActiveDays(
        year: Int,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): Map<BiologicalActivity, List<Range<LocalDate>>> {
        val climate = weather.getClimateClassification(location, elevation, calibrated)
        val temperatures = weather.getTemperatureRanges(year, location, elevation, calibrated)
        val containsLeapDay =
            temperatures.any { it.first.month == Month.FEBRUARY && it.first.dayOfMonth == 29 }

        val activeDays = mutableMapOf<BiologicalActivity, List<Range<LocalDate>>>()

        for (species in BiologicalActivity.entries) {
            if (climate.climateGroup in excludedClimates[species]!!) {
                activeDays[species] = listOf()
                continue
            }

            // If it doesn't drop below the base temperature, then they are active all year
            // TODO: Use average or max?
            if (temperatures.all { it.second.start.celsius().temperature >= phenology[species]!!.baseGrowingDegreeDaysTemperature.celsius().temperature }) {
                activeDays[species] =
                    listOf(Range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)))
                continue
            }

            val events = Phenology.getLifecycleEventDates(
                phenology[species]!!,
                Range(LocalDate.of(year - 1, 1, 1), LocalDate.of(year, 12, 31))
            ) { date ->
                (if (date.month == Month.FEBRUARY && date.dayOfMonth == 29 && !containsLeapDay) {
                    temperatures.first { it.first.month == Month.FEBRUARY && it.first.dayOfMonth == 28 }
                } else {
                    temperatures.first { it.first.month == date.month && it.first.dayOfMonth == date.dayOfMonth }
                }).second
            }

            activeDays[species] =
                getActivePeriodsForYear(year, events, EVENT_ACTIVE_START, EVENT_ACTIVE_END)
        }

        return activeDays
    }

    companion object {
        private const val EVENT_ACTIVE_START = "ACTIVE_START"
        private const val EVENT_ACTIVE_END = "ACTIVE_END"
    }

}