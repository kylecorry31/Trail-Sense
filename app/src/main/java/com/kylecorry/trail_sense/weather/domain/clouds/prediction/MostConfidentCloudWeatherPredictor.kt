package com.kylecorry.trail_sense.weather.domain.clouds.prediction

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService

class MostConfidentCloudWeatherPredictor : ICloudWeatherPredictor {
    private val cloudService = CloudService()

    override fun predict(clouds: List<ClassificationResult<CloudGenus>>): Weather {
        val mostConfident = clouds.maxByOrNull { it.confidence }?.value ?: return Weather.NoChange
        return cloudService.getWeather(mostConfident)
    }
}