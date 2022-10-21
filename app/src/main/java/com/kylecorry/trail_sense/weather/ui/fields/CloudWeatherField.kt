package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudDetailsService
import com.kylecorry.trail_sense.weather.ui.clouds.CloudDetailsModal

class CloudWeatherField(
    private val cloud: Reading<CloudGenus?>?
) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        cloud ?: return null
        val cloudDetailsService = CloudDetailsService(context)
        val name = cloudDetailsService.getCloudName(cloud.value)

        return ListItem(
            7,
            context.getString(R.string.clouds),
            icon = ResourceListIcon(R.drawable.cloudy),
            trailingText = name
        ) {
            CloudDetailsModal(context).show(cloud.value)
        }
    }
}