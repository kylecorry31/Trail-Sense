package com.kylecorry.trail_sense.weather.ui

import android.widget.ImageView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudListItem(
    private val type: CloudGenus,
    private val cloudRepo: CloudRepo,
    private val confidence: Float? = null,
    private val cloudService: CloudService = CloudService()
) {

    fun display(binding: ListItemCloudBinding) {
        val context = binding.root.context
        binding.name.text = type.name
        binding.description.text = cloudRepo.getCloudDescription(type)
        binding.cloudImg.setImageResource(cloudRepo.getCloudImage(type))
        val precipitation = cloudService.getPrecipitation(type)
        setPrecipitationActive(
            binding.precipitationHail,
            precipitation.containsAny(listOf(Precipitation.Hail, Precipitation.SmallHail))
        )
        setPrecipitationActive(
            binding.precipitationLightning,
            precipitation.contains(Precipitation.Lightning)
        )
        setPrecipitationActive(
            binding.precipitationRain,
            precipitation.containsAny(listOf(Precipitation.Rain, Precipitation.Drizzle))
        )
        setPrecipitationActive(
            binding.precipitationSnow,
            precipitation.containsAny(
                listOf(
                    Precipitation.Snow,
                    Precipitation.SnowPellets,
                    Precipitation.SnowGrains,
                    Precipitation.IcePellets
                )
            )
        )

        val formatter = FormatService(context)

        binding.confidence.isVisible = confidence != null
        if (confidence != null) {
            binding.confidence.text = formatter.formatPercentage(confidence * 100)
        }

        binding.precipitation.setOnClickListener {
            Alerts.dialog(
                context,
                cloudRepo.getCloudName(type),
                if (precipitation.isEmpty()) context.getString(R.string.precipitation_none) else precipitation.joinToString(
                    "\n"
                ) { formatter.formatPrecipitation(it) },
                cancelText = null
            )
        }

        binding.cloudImg.setOnClickListener {
            CustomUiUtils.showImage(
                context,
                cloudRepo.getCloudName(type),
                cloudRepo.getCloudImage(type)
            )
        }
    }

    private fun setPrecipitationActive(precipitation: ImageView, active: Boolean) {
        if (active) {
            CustomUiUtils.setImageColor(precipitation, null)
            precipitation.alpha = 1f
        } else {
            CustomUiUtils.setImageColor(
                precipitation,
                Resources.color(precipitation.context, R.color.colorSecondary)
            )
            precipitation.alpha = 0.02f
        }
    }

    private fun <T> List<T>.containsAny(values: List<T>): Boolean {
        return any { it in values }
    }


}