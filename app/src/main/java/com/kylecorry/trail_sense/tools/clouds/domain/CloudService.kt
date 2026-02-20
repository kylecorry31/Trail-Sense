package com.kylecorry.trail_sense.tools.clouds.domain

import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.domain.Probability
import com.kylecorry.trail_sense.shared.domain.probability

class CloudService {

    fun getPrecipitationProbability(cloud: CloudGenus?): Probability {
        if (cloud == null) {
            return probability(0f)
        }
        val chance = Meteorology.getPrecipitationChance(cloud)
        return probability(chance)
    }

    fun getPrecipitation(type: CloudGenus?): List<Precipitation> {
        if (type == null) {
            return emptyList()
        }
        return Meteorology.getPrecipitation(type)
    }

}