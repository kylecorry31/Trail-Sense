package com.kylecorry.trail_sense.tools.battery.domain

import com.kylecorry.andromeda.core.time.hours
import java.time.Duration

class PowerService {

    /**
     * Capacity can either be in mAh or percent, as long as both are consistent
     */
    fun getTimeUntilEmpty(capacity: Float, capacityDrainPerHour: Float): Duration? {
        if (capacityDrainPerHour >= 0) {
            return null
        }

        val hours = -(capacity / capacityDrainPerHour)

        return hours(hours)
    }

    fun getRates(readings: List<BatteryReading>, minDuration: Duration, hasCapacity: Boolean): List<Float> {
        if (readings.size < 2) {
            return emptyList()
        }

        val sorted = readings.sortedBy { it.time }

        val grouped = mutableListOf<Pair<Boolean, MutableList<BatteryReading>>>()

        for (i in 1 until sorted.size) {
            val charging = wasCharged(sorted[i - 1], sorted[i])

            val last = grouped.lastOrNull()

            val group = if (last == null || last.first != charging) {
                val newGroup = Pair(charging, mutableListOf<BatteryReading>())
                newGroup.second.add(sorted[i - 1])
                grouped.add(newGroup)
                newGroup
            } else {
                last
            }

            group.second.add(sorted[i])
        }

        return grouped
            .filter {
                Duration.between(
                    it.second.first().time,
                    it.second.last().time
                ) >= minDuration
            }
            .mapNotNull { getRate(it.second.first(), it.second.last(), hasCapacity) }

    }

    fun getRate(
        first: BatteryReading,
        second: BatteryReading,
        hasCapacity: Boolean = first.capacity != 0f
    ): Float? {
        val capacityDiff =
            if (hasCapacity) second.capacity - first.capacity else second.percent - first.percent
        val timeDiff = Duration.between(first.time, second.time).toMillis() / (1000f * 60f * 60f)

        if (timeDiff == 0f) {
            return null
        }

        return (capacityDiff / timeDiff)

    }

    fun wasCharged(first: BatteryReading, second: BatteryReading): Boolean {
        return second.isCharging || second.capacity > first.capacity || second.percent > first.percent
    }

    /**
     * Capacity can either be in mAh or percent, as long as both are consistent
     */
    fun getTimeUntilFull(
        capacity: Float,
        maxCapacity: Float,
        capacityGainPerHour: Float
    ): Duration? {
        if (capacityGainPerHour <= 0 && maxCapacity >= capacity) {
            return null
        }

        val remaining = maxCapacity - capacity

        val hours = remaining / capacityGainPerHour

        if (hours < 0){
            return Duration.ZERO
        }

        return hours(hours)
    }

    /**
     * The readings are pairs of percent to capacity
     * @return the maximum capacity (100%) in mAh
     */
    fun getMaxCapacity(readings: List<BatteryReading>): Float? {

        if (readings.isEmpty() || readings.first().capacity == 0f){
            return null
        }

        val fullReading = readings.filter { it.percent == 100f }.minByOrNull { it.capacity }?.capacity

        if (fullReading != null) {
            return fullReading
        }

        // Calculate the average mAh between percentages
        val percentGroupedReadings = readings.groupBy { it.percent }
            .map { it.key to it.value.map { it.capacity }.average().toFloat() }.sortedBy { it.first }

        val mAhPerPercent = mutableListOf<Float>()

        for (i in 1 until percentGroupedReadings.size) {
            val firstReading = percentGroupedReadings[i - 1]
            val secondReading = percentGroupedReadings[i]

            val diffPercent = secondReading.first - firstReading.first
            val diffCapacity = secondReading.second - firstReading.second

            mAhPerPercent.add(diffCapacity / diffPercent)
        }

        if (mAhPerPercent.isEmpty()) {
            return null
        }

        return mAhPerPercent.average().toFloat()
    }

}