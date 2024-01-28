package com.kylecorry.trail_sense.tools.clouds.ui

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService

class CloudImageModal(private val context: Context) {

    private val details = CloudDetailsService(context)

    fun show(cloud: CloudGenus?) {
        if (cloud != null) {
            Alerts.image(
                context,
                details.getCloudName(cloud),
                details.getCloudImage(cloud)
            )
        }
    }

}