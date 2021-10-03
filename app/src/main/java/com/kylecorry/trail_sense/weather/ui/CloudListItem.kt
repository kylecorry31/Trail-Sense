package com.kylecorry.trail_sense.weather.ui

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.meteorology.clouds.CloudHeight
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudListItem(
    private val type: CloudType,
    private val cloudRepo: CloudRepo,
    private val cloudService: CloudService = CloudService()
) {

    fun display(binding: ListItemCloudBinding) {
        val context = binding.root.context
        binding.name.text = type.name
        binding.description.text = cloudRepo.getCloudDescription(type)
        binding.cloudImg.setImageResource(cloudRepo.getCloudImage(type))
        val weather = cloudService.getCloudPrecipitation(type)
        binding.precipitation.setImageResource(cloudRepo.getCloudWeatherIcon(weather))

        when (type.height) {
            CloudHeight.Low -> {
                binding.cloudHeightHigh.setTextColor(
                    Resources.androidTextColorSecondary(
                        context
                    )
                )
                binding.cloudHeightMiddle.setTextColor(
                    Resources.androidTextColorSecondary(
                        context
                    )
                )
                binding.cloudHeightLow.setTextColor(
                    Resources.color(
                        context,
                        R.color.colorPrimary
                    )
                )
                binding.cloudHeightHigh.alpha = 0.25f
                binding.cloudHeightMiddle.alpha = 0.25f
                binding.cloudHeightLow.alpha = 1f
            }
            CloudHeight.Middle -> {
                binding.cloudHeightHigh.setTextColor(
                    Resources.androidTextColorSecondary(
                        context
                    )
                )
                binding.cloudHeightMiddle.setTextColor(
                    Resources.color(
                        context,
                        R.color.colorPrimary
                    )
                )
                binding.cloudHeightLow.setTextColor(
                    Resources.androidTextColorSecondary(
                        context
                    )
                )
                binding.cloudHeightHigh.alpha = 0.25f
                binding.cloudHeightMiddle.alpha = 1f
                binding.cloudHeightLow.alpha = 0.25f
            }
            CloudHeight.High -> {
                binding.cloudHeightHigh.setTextColor(
                    Resources.color(
                        context,
                        R.color.colorPrimary
                    )
                )
                binding.cloudHeightMiddle.setTextColor(
                    Resources.androidTextColorSecondary(
                        context
                    )
                )
                binding.cloudHeightLow.setTextColor(
                    Resources.androidTextColorSecondary(
                        context
                    )
                )
                binding.cloudHeightHigh.alpha = 1f
                binding.cloudHeightMiddle.alpha = 0.25f
                binding.cloudHeightLow.alpha = 0.25f
            }
        }

        binding.precipitation.setOnClickListener {
            Alerts.dialog(
                context,
                cloudRepo.getCloudName(type),
                cloudRepo.getCloudWeatherString(weather),
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


}