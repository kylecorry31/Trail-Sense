package com.kylecorry.trail_sense.weather.ui.clouds

import android.content.Context
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
        binding.description.text = cloudRepo.getCloudForecast(type)
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

        binding.root.setOnClickListener {
            Alerts.dialog(
                context,
                cloudRepo.getCloudName(type),
                cloudRepo.getCloudDescription(type) + "\n\n" +
                        getPrecipitationDescription(context, type, precipitation, formatter),
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

    private fun getPrecipitationDescription(
        context: Context,
        type: CloudGenus,
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

    private fun setPrecipitationActive(precipitation: ImageView, active: Boolean) {
        if (active) {
            CustomUiUtils.setImageColor(precipitation, null)
            precipitation.alpha = 1f
        } else {
            CustomUiUtils.setImageColor(
                precipitation,
                Resources.androidTextColorSecondary(precipitation.context)
            )
            precipitation.alpha = 0.1f
        }
    }

    private fun <T> List<T>.containsAny(values: List<T>): Boolean {
        return any { it in values }
    }


}