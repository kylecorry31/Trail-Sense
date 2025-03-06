package com.kylecorry.trail_sense.tools.clouds.infrastructure

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class CloudDetailsService(private val context: Context) {

    fun getCloudDescription(type: CloudGenus?): String {
        return when (type) {
            CloudGenus.Cirrocumulus -> context.getString(R.string.cirrocumulus_desc)
            CloudGenus.Cirrostratus -> context.getString(R.string.cirrostratus_desc)
            CloudGenus.Altocumulus -> context.getString(R.string.altocumulus_desc)
            CloudGenus.Altostratus -> context.getString(R.string.altostratus_desc)
            CloudGenus.Nimbostratus -> context.getString(R.string.nimbostratus_desc)
            CloudGenus.Stratus -> context.getString(R.string.stratus_desc)
            CloudGenus.Stratocumulus -> context.getString(R.string.stratocumulus_desc)
            CloudGenus.Cumulus -> context.getString(R.string.cumulus_desc)
            CloudGenus.Cumulonimbus -> context.getString(R.string.cumulonimbus_desc)
            CloudGenus.Cirrus -> context.getString(R.string.cirrus_desc)
            null -> context.getString(R.string.no_clouds)
        }
    }

    fun getCloudForecast(type: CloudGenus?): String {
        return when (type) {
            CloudGenus.Cirrocumulus -> context.getString(
                R.string.cloud_precipitation_forecast_hour_range,
                8,
                12
            )

            CloudGenus.Cirrostratus -> context.getString(
                R.string.cloud_precipitation_forecast_hour_range,
                10,
                15
            )

            CloudGenus.Altocumulus -> context.getString(R.string.altocumulus_forecast, 12)
            CloudGenus.Altostratus -> context.getString(R.string.altostratus_forecast, 8)
            CloudGenus.Nimbostratus -> context.getString(R.string.nimbostratus_forecast, 4)
            CloudGenus.Stratus -> context.getString(R.string.cloud_stratus_forecast, 3)
            CloudGenus.Stratocumulus -> context.getString(R.string.cloud_fair_forecast_hours, 3)
            CloudGenus.Cumulus -> context.getString(R.string.cumulus_forecast, 3)
            CloudGenus.Cumulonimbus -> context.getString(R.string.cumulonimbus_forecast)
            CloudGenus.Cirrus -> context.getString(
                R.string.cloud_precipitation_forecast_hour_range,
                12,
                24
            )

            null -> "-"
        }
    }

    fun getCloudName(type: CloudGenus?): String {
        return when (type) {
            CloudGenus.Cirrus -> context.getString(R.string.cirrus)
            CloudGenus.Cirrocumulus -> context.getString(R.string.cirrocumulus)
            CloudGenus.Cirrostratus -> context.getString(R.string.cirrostratus)
            CloudGenus.Altocumulus -> context.getString(R.string.altocumulus)
            CloudGenus.Altostratus -> context.getString(R.string.altostratus)
            CloudGenus.Nimbostratus -> context.getString(R.string.nimbostratus)
            CloudGenus.Stratus -> context.getString(R.string.stratus)
            CloudGenus.Stratocumulus -> context.getString(R.string.stratocumulus)
            CloudGenus.Cumulus -> context.getString(R.string.cumulus)
            CloudGenus.Cumulonimbus -> context.getString(R.string.cumulonimbus)
            null -> context.getString(R.string.clouds_clear)
        }
    }

    fun getCloudImage(context: Context, type: CloudGenus?): Drawable? {
        val files = AppServiceRegistry.get<FileSubsystem>()
        return when (type) {
            CloudGenus.Cirrus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cirrus.webp")
            CloudGenus.Cirrocumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cirrocumulus.webp")
            CloudGenus.Cirrostratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cirrostratus.webp")
            CloudGenus.Altocumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/altocumulus.webp")
            CloudGenus.Altostratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/altostratus.webp")
            CloudGenus.Nimbostratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/nimbostratus.webp")
            CloudGenus.Stratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/stratus.webp")
            CloudGenus.Stratocumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/stratocumulus.webp")
            CloudGenus.Cumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cumulus.webp")
            CloudGenus.Cumulonimbus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cumulonimbus.webp")
            null -> Resources.drawable(context, R.drawable.rectangle)
                ?.also { it.setTint(Color.parseColor("#84bfdf")) }
        }
    }

}