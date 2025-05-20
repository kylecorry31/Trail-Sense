package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import java.time.LocalDate

// TODO: Cooldown driven events
// TODO: Frost driven events
data class LifecycleEventFactors(
    val cumulativeGrowingDegreeDays: Float,
    val temperatureHistory30Days: List<Range<Temperature>>,
    val cumulativeGrowingDegreeDayHistory30Days: List<Float>
)

interface LifecycleEventTrigger {
    fun isTriggered(factors: LifecycleEventFactors): Boolean
}

enum class GrowingDegreeDaysCalculationType {
    /**
     * Always uses the minimum and maximum temperature
     */
    MinMax,

    /**
     * Uses the minimum and maximum temperature, but if min < base it will use base instead of min
     */
    BaseMax
}

data class LifecycleEvent(val name: String, val trigger: LifecycleEventTrigger)
data class SpeciesPhenology(
    val baseGrowingDegreeDaysTemperature: Temperature,
    val events: List<LifecycleEvent>,
    val growingDegreeDaysCap: Float = Float.MAX_VALUE,
    val growingDegreeDaysCalculationType: GrowingDegreeDaysCalculationType = GrowingDegreeDaysCalculationType.MinMax
)

// Common triggers
class MinimumGrowingDegreeDaysTrigger(
    minimum: Float,
    units: TemperatureUnits = TemperatureUnits.C
) : LifecycleEventTrigger {

    private val minC = if (units == TemperatureUnits.C) {
        minimum
    } else {
        minimum * 5 / 9f
    }

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        return factors.cumulativeGrowingDegreeDays >= minC
    }
}

class BelowTemperatureTrigger(
    averageLowTemperature: Temperature = Temperature.celsius(0f),
) : LifecycleEventTrigger {

    private val averageLowC = averageLowTemperature.celsius().temperature

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        val aboveFreezing =
            factors.temperatureHistory30Days.indexOfFirst { it.start.celsius().temperature > averageLowC }
        val belowFreezing =
            factors.temperatureHistory30Days.indexOfFirst { it.start.celsius().temperature <= averageLowC }
        // Drops below freezing
        return aboveFreezing != -1 && belowFreezing > aboveFreezing
    }

}

class AboveTemperatureTrigger(
    averageHighTemperature: Temperature = Temperature.celsius(0f),
) : LifecycleEventTrigger {

    private val averageHighC = averageHighTemperature.celsius().temperature

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        val aboveTemperature =
            factors.temperatureHistory30Days.indexOfFirst { it.end.celsius().temperature >= averageHighC }
        val belowTemperature =
            factors.temperatureHistory30Days.indexOfFirst { it.end.celsius().temperature < averageHighC }
        // Rises above temperature
        return belowTemperature != -1 && aboveTemperature > belowTemperature
    }

}

class CoolingGrowingDegreeDaysTrigger(
    minimum: Float,
    units: TemperatureUnits = TemperatureUnits.C,
    private val days: Int = 30
) : LifecycleEventTrigger {

    private val minC = if (units == TemperatureUnits.C) {
        minimum
    } else {
        minimum * 5 / 9f
    }

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        // Get the difference between each cumulative GDD
        val gdd = mutableListOf<Float>()
        for (i in 1 until factors.cumulativeGrowingDegreeDayHistory30Days.size) {
            gdd.add(
                factors.cumulativeGrowingDegreeDayHistory30Days[i] - factors.cumulativeGrowingDegreeDayHistory30Days[i - 1]
            )
        }

        val cumulative = gdd.takeLast(days).sum()
        return cumulative <= minC
    }
}

class MultiTrigger(private vararg val triggers: LifecycleEventTrigger) : LifecycleEventTrigger {
    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        return triggers.all { it.isTriggered(factors) }
    }

}

// TODO: Extract to Sol
object Phenology {

    fun getGrowingDegreeDays(
        temperature: Range<Temperature>,
        baseTemperature: Temperature,
        limit: Float = Float.MAX_VALUE,
        calculationType: GrowingDegreeDaysCalculationType = GrowingDegreeDaysCalculationType.MinMax
    ): Float {

        val max = temperature.end.celsius().temperature.coerceAtMost(limit)
        var min = temperature.start.celsius().temperature

        if (calculationType == GrowingDegreeDaysCalculationType.BaseMax && min < baseTemperature.celsius().temperature) {
            min = baseTemperature.celsius().temperature
        }

        val average = (max + min) / 2

        return (average - baseTemperature.celsius().temperature).coerceAtLeast(0f)
    }

    fun getCumulativeGrowingDegreeDays(
        dates: List<LocalDate>,
        baseTemperature: Temperature,
        temperatureProvider: (LocalDate) -> Range<Temperature>,
        limit: Float = Float.MAX_VALUE,
        calculationType: GrowingDegreeDaysCalculationType = GrowingDegreeDaysCalculationType.MinMax
    ): List<Pair<LocalDate, Float>> {
        val earliestDate = dates.minOrNull() ?: return emptyList()

        // Search the full year prior to the first date for the lowest temperature
        var startDate = earliestDate.minusYears(1)
        var lowestTemperature = temperatureProvider(startDate)
        var currentDate = startDate.plusDays(1)
        while (currentDate < earliestDate) {
            val temperature = temperatureProvider(currentDate)
            if (temperature.start.celsius().temperature < lowestTemperature.start.celsius().temperature) {
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
                limit,
                calculationType
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
        temperatureProvider: (LocalDate) -> Range<Temperature>
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
            phenology.growingDegreeDaysCap,
            phenology.growingDegreeDaysCalculationType
        )

        val lifecycleEvents = mutableListOf<Pair<LocalDate, LifecycleEvent>>()
        val hits = mutableSetOf<LifecycleEvent>()
        val temperatureBuffer = RingBuffer<Range<Temperature>>(30)
        val gddBuffer = RingBuffer<Float>(30)
        var lastGdd = 0f
        for (date in gdd) {
            // New year starting
            if (lastGdd > date.second) {
                hits.clear()
                temperatureBuffer.clear()
                gddBuffer.clear()
            }
            lastGdd = date.second
            temperatureBuffer.add(temperatureProvider(date.first))
            gddBuffer.add(date.second)

            val factors = LifecycleEventFactors(
                date.second,
                temperatureBuffer.toList(),
                gddBuffer.toList()
            )

            for (event in phenology.events) {
                if (event in hits) {
                    continue
                }
                if (event.trigger.isTriggered(factors)) {
                    hits.add(event)
                    lifecycleEvents.add(Pair(date.first, event))
                }
            }
        }
        return lifecycleEvents
    }
}