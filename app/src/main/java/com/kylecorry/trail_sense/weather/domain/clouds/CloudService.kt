package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.clouds.CloudCategory
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

    fun getCloudsInCategory(category: CloudCategory): List<CloudGenus> {
        return CloudGenus.values().filter { category in it.categories }
    }

    fun getPrecipitation(type: CloudGenus): List<Precipitation> {
        return when (type) {
            CloudGenus.Altostratus -> listOf(
                Precipitation.Rain,
                Precipitation.Snow,
                Precipitation.IcePellets
            )
            CloudGenus.Nimbostratus -> listOf(
                Precipitation.Rain,
                Precipitation.Snow,
                Precipitation.IcePellets
            )
            CloudGenus.Stratus -> listOf(
                Precipitation.Drizzle,
                Precipitation.Snow,
                Precipitation.SnowGrains
            )
            CloudGenus.Stratocumulus -> listOf(
                Precipitation.Rain,
                Precipitation.Drizzle,
                Precipitation.Snow,
                Precipitation.SnowPellets
            )
            CloudGenus.Cumulus -> listOf(
                Precipitation.Rain,
                Precipitation.Snow,
                Precipitation.SnowPellets
            )
            CloudGenus.Cumulonimbus -> listOf(
                Precipitation.Rain,
                Precipitation.Snow,
                Precipitation.SnowPellets,
                Precipitation.Hail,
                Precipitation.SmallHail,
                Precipitation.Lightning
            )
            else -> emptyList()
        }
    }
}