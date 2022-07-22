package com.kylecorry.trail_sense.weather.domain.clouds.prediction

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService

class GroupedCloudWeatherPredictor : ICloudWeatherPredictor {

    private val cloudService = CloudService()

    override fun predict(clouds: List<ClassificationResult<CloudGenus>>): Weather {
        val predictions = mutableMapOf<Weather, Float>()
        for (cloud in clouds) {
            val weather = cloudService.getWeather(cloud.value)
            val newValue = (predictions[weather] ?: 0f) + cloud.confidence
            predictions[weather] = newValue
        }
        return predictions.maxByOrNull { it.value }?.key ?: Weather.NoChange
    }
}