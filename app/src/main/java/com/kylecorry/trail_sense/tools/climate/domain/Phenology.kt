package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Temperature
import java.time.LocalDate

// TODO: Cooldown driven events
// TODO: Frost driven events
data class LifecycleEvent(val name: String, val gddThreshold: Float)
data class SpeciesPhenology(
    val baseGrowingDegreeDaysTemperature: Temperature,
    val events: List<LifecycleEvent>,
    val growingDegreeDaysCap: Float = Float.MAX_VALUE
)

// TODO: Extract to Sol
object Phenology {

    fun getGrowingDegreeDays(
        temperature: Temperature,
        baseTemperature: Temperature,
        limit: Float = Float.MAX_VALUE
    ): Float {
        return (temperature.celsius().temperature - baseTemperature.celsius().temperature).coerceIn(
            0f,
            limit
        )
    }

    fun getCumulativeGrowingDegreeDays(
        dates: List<LocalDate>,
        baseTemperature: Temperature,
        temperatureProvider: (LocalDate) -> Temperature,
        limit: Float = Float.MAX_VALUE
    ): List<Pair<LocalDate, Float>> {
        val earliestDate = dates.minOrNull() ?: return emptyList()

        // Search the full year prior to the first date for the lowest temperature
        var startDate = earliestDate.minusYears(1)
        var lowestTemperature = temperatureProvider(startDate)
        var currentDate = startDate.plusDays(1)
        while (currentDate < earliestDate) {
            val temperature = temperatureProvider(currentDate)
            if (temperature.celsius().temperature < lowestTemperature.celsius().temperature) {
                lowestTemperature = temperature
                startDate = currentDate
            }
            currentDate = currentDate.plusDays(1)
        }

        // Calculate from that day forward (reset every year)
        currentDate = earliestDate
        val gdd = mutableListOf<Pair<LocalDate, Float>>()
        val queue = dates.toMutableSet()
        var cumulative = 0f
        while (queue.isNotEmpty()) {
            if (currentDate >= startDate.plusYears(1)) {
                startDate = startDate.plusYears(1)
                cumulative = 0f
            }
            cumulative += getGrowingDegreeDays(
                temperatureProvider(currentDate),
                baseTemperature,
                limit
            )
            if (currentDate in queue) {
                queue.remove(currentDate)
                gdd.add(Pair(currentDate, cumulative))
            }
            currentDate = currentDate.plusDays(1)
        }
        return gdd
    }

    fun getLifecycleEventDates(
        phenology: SpeciesPhenology,
        dateRange: Range<LocalDate>,
        temperatureProvider: (LocalDate) -> Temperature
    ): List<Pair<LocalDate, LifecycleEvent>> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = dateRange.start
        while (currentDate <= dateRange.end) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        val gdd = getCumulativeGrowingDegreeDays(
            dates,
            phenology.baseGrowingDegreeDaysTemperature,
            temperatureProvider,
            phenology.growingDegreeDaysCap
        )

        val lifecycleEvents = mutableListOf<Pair<LocalDate, LifecycleEvent>>()
        val hits = mutableSetOf<LifecycleEvent>()
        var lastGdd = 0f
        for (date in gdd) {
            if (lastGdd > date.second) {
                hits.clear()
            }
            lastGdd = date.second

            for (event in phenology.events) {
                if (event in hits) {
                    continue
                }
                if (date.second >= event.gddThreshold) {
                    hits.add(event)
                    lifecycleEvents.add(Pair(date.first, event))
                }
            }
        }
        return lifecycleEvents
    }
}