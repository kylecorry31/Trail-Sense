package com.kylecorry.trail_sense.weather.ui.clouds

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudDetailsService

class CloudDetailsModal(private val context: Context) {

    private val cloudService = CloudService()
    private val details = CloudDetailsService(context)
    private val formatter = FormatService.getInstance(context)

    fun show(cloud: CloudGenus?) {
        val precipitation = cloudService.getPrecipitation(cloud)
        Alerts.dialog(
            context,
            details.getCloudName(cloud),
            details.getCloudDescription(cloud) + "\n\n" +
                    details.getCloudForecast(cloud) + "\n\n" +
                    getPrecipitationDescription(
                        context,
                        cloud,
                        precipitation,
                        formatter
                    ),
            cancelText = null
        )
    }

    private fun getPrecipitationDescription(
        context: Context,
        type: CloudGenus?,
        precipitation: List<Precipitation>,
        formatter: FormatService
    ): String {
        return context.getString(
            R.string.precipitation_chance,
            formatter.formatProbability(cloudService.getPrecipitationProbability(type))
        ) + "\n\n" +
                if (precipitation.isEmpty()) context.getString(R.string.precipitation_none) else precipitation.joinToString(
                    "\n"
                ) { formatter.formatPrecipitation(it) }
    }

}