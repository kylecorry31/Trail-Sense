package com.kylecorry.trail_sense.weather.domain.clouds.prediction

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult

interface ICloudWeatherPredictor {
    fun predict(clouds: List<ClassificationResult<CloudGenus>>): Weather
}