package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.ecology.Ecology
import com.kylecorry.sol.time.Time.daysUntil
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.andromeda_temp.mergeIntersecting
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import java.time.Duration
import java.time.LocalDate

class PhenologyService(private val weather: IWeatherSubsystem) {

    suspend fun getYearlyActiveDays(
        year: Int,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): Map<BiologicalActivityType, List<Range<LocalDate>>> = onDefault {
        val climate = weather.getClimateClassification(location, elevation, calibrated)
        val temperatures = weather.getTemperatureRanges(year, location, elevation, calibrated)
        val activeDays = mutableMapOf<SpeciesPhenology, List<Range<LocalDate>>>()
        val temperatureMap = temperatures.associateBy { it.first }

        for (species in SpeciesPhenology.ENTRIES) {
            if (climate.code in species.excludedClimates) {
                activeDays[species] = listOf()
                continue
            }

            val events = Ecology.getLifecycleEventDates(
                species.events,
                Range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)),
                // TODO: This isn't needed right now, but it should be an interpolated version of astronomy get daylight length (for performance)
                { Duration.ofHours(12) }
            ) { date ->
                val newDate = date.withYear(year)
                temperatureMap[newDate]?.second ?: Range(Temperature.zero, Temperature.zero)
            }.toMutableList()

            activeDays[species] =
                Ecology.getActivePeriodsForYear(year, events, EVENT_ACTIVE_START, EVENT_ACTIVE_END)
                    .filter { it.start.daysUntil(it.end) > 1 }
        }

        // Merge the active periods for similar biological activity
        val biologicalActivity = mutableMapOf<BiologicalActivityType, List<Range<LocalDate>>>()
        for (type in BiologicalActivityType.entries) {
            val matches = activeDays.entries.filter { it.key.type == type }
            val allDateRanges = matches.flatMap { it.value }.sortedBy { it.start }
            biologicalActivity[type] = allDateRanges.mergeIntersecting()
        }

        biologicalActivity
    }

    companion object {
        const val EVENT_ACTIVE_START = "ACTIVE_START"
        const val EVENT_ACTIVE_END = "ACTIVE_END"
    }

}