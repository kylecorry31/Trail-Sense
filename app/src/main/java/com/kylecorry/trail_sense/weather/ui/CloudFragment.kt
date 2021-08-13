package com.kylecorry.trail_sense.weather.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import com.kylecorry.trailsensecore.domain.weather.clouds.CloudHeight
import com.kylecorry.trailsensecore.domain.weather.clouds.CloudType
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trailsensecore.infrastructure.view.ListView

class CloudFragment : BoundFragment<FragmentCloudsBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<CloudType>
    private val weatherService = WeatherService()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.cloudList, R.layout.list_item_cloud) { itemView, item ->
            val itemBinding = ListItemCloudBinding.bind(itemView)
            itemBinding.name.text = item.name
            itemBinding.description.text = cloudRepo.getCloudDescription(item)
            itemBinding.cloudImg.setImageResource(cloudRepo.getCloudImage(item))
            val weather = weatherService.getCloudPrecipitation(item)
            itemBinding.precipitation.setImageResource(cloudRepo.getCloudWeatherIcon(weather))

            when(item.height){
                CloudHeight.Low -> {
                    itemBinding.cloudHeightHigh.setTextColor(
                        UiUtils.androidTextColorSecondary(
                            requireContext()
                        )
                    )
                    itemBinding.cloudHeightMiddle.setTextColor(
                        UiUtils.androidTextColorSecondary(
                            requireContext()
                        )
                    )
                    itemBinding.cloudHeightLow.setTextColor(
                        UiUtils.color(
                            requireContext(),
                            R.color.colorPrimary
                        )
                    )
                    itemBinding.cloudHeightHigh.alpha = 0.25f
                    itemBinding.cloudHeightMiddle.alpha = 0.25f
                    itemBinding.cloudHeightLow.alpha = 1f
                }
                CloudHeight.Middle -> {
                    itemBinding.cloudHeightHigh.setTextColor(
                        UiUtils.androidTextColorSecondary(
                            requireContext()
                        )
                    )
                    itemBinding.cloudHeightMiddle.setTextColor(
                        UiUtils.color(
                            requireContext(),
                            R.color.colorPrimary
                        )
                    )
                    itemBinding.cloudHeightLow.setTextColor(
                        UiUtils.androidTextColorSecondary(
                            requireContext()
                        )
                    )
                    itemBinding.cloudHeightHigh.alpha = 0.25f
                    itemBinding.cloudHeightMiddle.alpha = 1f
                    itemBinding.cloudHeightLow.alpha = 0.25f
                }
                CloudHeight.High -> {
                    itemBinding.cloudHeightHigh.setTextColor(
                        UiUtils.color(
                            requireContext(),
                            R.color.colorPrimary
                        )
                    )
                    itemBinding.cloudHeightMiddle.setTextColor(
                        UiUtils.androidTextColorSecondary(
                            requireContext()
                        )
                    )
                    itemBinding.cloudHeightLow.setTextColor(
                        UiUtils.androidTextColorSecondary(
                            requireContext()
                        )
                    )
                    itemBinding.cloudHeightHigh.alpha = 1f
                    itemBinding.cloudHeightMiddle.alpha = 0.25f
                    itemBinding.cloudHeightLow.alpha = 0.25f
                }
            }

            itemBinding.precipitation.setOnClickListener {
                UiUtils.alert(requireContext(), cloudRepo.getCloudName(item), cloudRepo.getCloudWeatherString(weather), getString(R.string.dialog_ok))
            }

            itemBinding.cloudImg.setOnClickListener {
                imageAlert(
                    requireContext(), cloudRepo.getCloudName(item), cloudRepo.getCloudImage(
                        item
                    ), getString(R.string.dialog_ok)
                )
            }

        }

        listView.addLineSeparator()
        listView.setData(cloudRepo.getClouds().sortedByDescending { it.height })
    }


    // TODO: Extract this
    private fun imageAlert(
        context: Context,
        title: String,
        @DrawableRes image: Int,
        buttonOk: String,
        onClose: (() -> Unit)? = null
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val view = LinearLayout(context)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        view.layoutParams = params
        val imageView = ImageView(context)
        val imageParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        imageParams.gravity = Gravity.CENTER
        imageView.layoutParams = imageParams
        imageView.setImageResource(image)
        view.addView(imageView)

        builder.apply {
            setView(view)
            setTitle(title)
            setPositiveButton(
                buttonOk
            ) { dialog, _ ->
                onClose?.invoke()
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
        return dialog
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudsBinding {
        return FragmentCloudsBinding.inflate(layoutInflater, container, false)
    }


}