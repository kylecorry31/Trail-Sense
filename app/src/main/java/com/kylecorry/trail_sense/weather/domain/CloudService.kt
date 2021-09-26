package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.clouds.CloudCover
import com.kylecorry.sol.science.meteorology.clouds.CloudService
import com.kylecorry.sol.science.meteorology.clouds.ICloudService

class CloudService(private val baseCloudService: ICloudService = CloudService()) {

    fun classifyCloudCover(percent: Float): CloudCover {
        return baseCloudService.getCloudCover(percent)
    }

}