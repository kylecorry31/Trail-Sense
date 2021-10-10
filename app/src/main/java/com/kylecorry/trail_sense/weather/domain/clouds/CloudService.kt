package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.sol.science.meteorology.Precipitation
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
}