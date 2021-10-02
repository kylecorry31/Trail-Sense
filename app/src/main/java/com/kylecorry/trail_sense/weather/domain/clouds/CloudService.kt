package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.statistics.StatisticsService
import com.kylecorry.sol.science.meteorology.clouds.CloudCover
import com.kylecorry.sol.science.meteorology.clouds.CloudService
import com.kylecorry.sol.science.meteorology.clouds.ICloudService
import com.kylecorry.sol.units.Reading
import java.time.Duration

class CloudService(private val baseCloudService: ICloudService = CloudService()) {

    private val statistics = StatisticsService()

    fun classifyCloudCover(percent: Float): CloudCover {
        return baseCloudService.getCloudCover(percent)
    }

    fun forecastClouds(readings: List<Reading<CloudObservation>>): Float {
        return getTendency(readings.map { Reading(it.value.coverage, it.time) })
    }

    private fun getTendency(readings: List<Reading<Float>>): Float {
        val first = readings.firstOrNull() ?: return 0f
        val normalizedReadings = readings.map {
            val hours = Duration.between(first.time, it.time).seconds / 3600f
            Vector2(hours, it.value)
        }
        return statistics.slope(normalizedReadings)
    }

}