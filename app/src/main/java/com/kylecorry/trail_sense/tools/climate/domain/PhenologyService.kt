package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import java.time.LocalDate
import java.time.Month

class PhenologyService(private val weather: IWeatherSubsystem) {

    // https://www.nrcc.cornell.edu/industry/mosquito/degreedays.html
    private val MOSQUITO_PHENOLOGY = SpeciesPhenology(
        Temperature.celsius(10f),
        listOf(LifecycleEvent("ADULT_EMERGENCE", 230f * 5 / 9f))
    )

    suspend fun getYearlyMosquitoActiveDays(
        year: Int,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): List<Range<LocalDate>> {
        // TODO: Factor in climate zone
        val temperatures = weather.getTemperatureRanges(year, location, elevation, calibrated)
        val containsLeapDay =
            temperatures.any { it.first.month == Month.FEBRUARY && it.first.dayOfMonth == 29 }
        val events = Phenology.getLifecycleEventDates(
            MOSQUITO_PHENOLOGY,
            Range(LocalDate.of(year - 1, 1, 1), LocalDate.of(year, 12, 31))
        ) { date ->
            // TODO: Make Phenology operate on min/max temps instead
            val match =
                if (date.month == Month.FEBRUARY && date.dayOfMonth == 29 && !containsLeapDay) {
                    temperatures.first { it.first.month == Month.FEBRUARY && it.first.dayOfMonth == 28 }
                } else {
                    temperatures.first { it.first.month == date.month && it.first.dayOfMonth == date.dayOfMonth }
                }
            var low = match.second.start.celsius().temperature
            val high = match.second.end.celsius().temperature

            // This method seems to work better
            if (low < MOSQUITO_PHENOLOGY.baseGrowingDegreeDaysTemperature.celsius().temperature){
                low = MOSQUITO_PHENOLOGY.baseGrowingDegreeDaysTemperature.celsius().temperature
            }

            Temperature.celsius((low + high) / 2)
        }

        val startDates = events.filter { it.second.name == "ADULT_EMERGENCE" }.map { it.first }
            .filter { it.year == year }

        if (startDates.isEmpty()) {
            return emptyList()
        }

        // Find first frost date (first dates where temperatures goes from above freezing to below freezing)
        val frostMinimum = 5f
        val frostDate = temperatures.zipWithNext()
            .firstOrNull { it.first.second.start.celsius().temperature > frostMinimum && it.second.second.start.celsius().temperature <= frostMinimum }?.first?.first

        // If it's null, check the last date against the first

        val activePeriods = mutableListOf<Range<LocalDate>>()

        for (startDate in startDates) {
            val nextFrostDate =
                if (frostDate != null && frostDate > startDate) frostDate else LocalDate.of(
                    year,
                    12,
                    31
                )
            activePeriods.add(Range(startDate, nextFrostDate))
        }

        return activePeriods
    }

}