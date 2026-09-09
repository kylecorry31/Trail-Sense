package com.kylecorry.trail_sense.tools.battery.domain

import com.kylecorry.sol.time.Time.hours
import java.time.Duration
import kotlin.math.absoluteValue

class PowerService {

    /**
     * Capacity can either be in mAh or percent, as long as both are consistent
     */
    fun getTimeUntilEmpty(capacity: Float, capacityDrainPerHour: Float): Duration? {
        if (capacityDrainPerHour >= 0) {
            return null
        }

        val hours = -(capacity / capacityDrainPerHour)

        return hours(hours.toDouble())
    }

    fun getDeepSleep(readings: List<BatteryReading>): DeepSleep? {
        val sorted = readings
            .filter { it.uptime != null && it.elapsedRealtime != null }
            .sortedBy { it.time }

        var totalElapsed = 0L
        var totalSleep = 0L

        for (i in 1 until sorted.size) {
            val previous = sorted[i - 1]
            val current = sorted[i]
            val elapsed = current.elapsedRealtime!!.toMillis() - previous.elapsedRealtime!!.toMillis()
            val awake = current.uptime!!.toMillis() - previous.uptime!!.toMillis()

            // The clocks reset on reboot, so only trust intervals where they still track wall time
            val wallClock = Duration.between(previous.time, current.time).toMillis()
            if (elapsed <= 0 || awake < 0 || (elapsed - wallClock).absoluteValue > REBOOT_TOLERANCE_MILLIS) {
                continue
            }

            totalElapsed += elapsed
            totalSleep += (elapsed - awake).coerceIn(0, elapsed)
        }

        if (totalElapsed == 0L) {
            return null
        }

        return DeepSleep(100f * totalSleep / totalElapsed, Duration.ofMillis(totalElapsed))
    }

    fun getRates(
        readings: List<BatteryReading>,
        minDuration: Duration,
        hasCapacity: Boolean
    ): List<Float> {
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

    private fun getRate(
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

    private fun wasCharged(first: BatteryReading, second: BatteryReading): Boolean {
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

        if (hours < 0) {
            return Duration.ZERO
        }

        return hours(hours.toDouble())
    }

    companion object {
        private const val REBOOT_TOLERANCE_MILLIS = 5 * 60 * 1000L
    }

}
