package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.science.meteorology.clouds.CloudService
import com.kylecorry.sol.science.meteorology.clouds.ICloudService
import com.kylecorry.trail_sense.shared.domain.Probability
import com.kylecorry.trail_sense.shared.domain.probability

class CloudService(private val baseCloudService: ICloudService = CloudService()) {

    fun getPrecipitationProbability(cloud: CloudGenus): Probability {
        val chance = baseCloudService.getPrecipitationChance(cloud)
        return probability(chance)
    }

    fun getPrecipitation(type: CloudGenus): List<Precipitation> {
        return baseCloudService.getPrecipitation(type)
    }

    fun getWeather(type: CloudGenus): Weather {
        return when (type) {
            CloudGenus.Cirrus -> Weather.ImprovingSlow
            CloudGenus.Cirrocumulus -> Weather.ImprovingSlow
            CloudGenus.Cirrostratus -> Weather.WorseningSlow // 12 - 24 hours before precipitation
            CloudGenus.Altocumulus -> Weather.WorseningSlow // Before a thunderstorm
            CloudGenus.Altostratus -> Weather.WorseningSlow // Rain storm on the way
            CloudGenus.Nimbostratus -> Weather.Storm
            CloudGenus.Stratus -> Weather.NoChange
            CloudGenus.Stratocumulus -> Weather.ImprovingSlow
            CloudGenus.Cumulus -> Weather.NoChange // Either fair weather or stormy weather
            CloudGenus.Cumulonimbus -> Weather.Storm
        }
    }
}