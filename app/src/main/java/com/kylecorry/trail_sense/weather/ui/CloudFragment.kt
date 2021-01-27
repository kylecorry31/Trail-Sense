package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import com.kylecorry.trailsensecore.domain.weather.clouds.CloudHeight
import com.kylecorry.trailsensecore.domain.weather.clouds.CloudType
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView

class CloudFragment : Fragment() {

    private var _binding: FragmentCloudsBinding? = null
    private val binding get() = _binding!!
    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<CloudType>
    private val weatherService = WeatherService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloudsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

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
                    itemBinding.cloudHeightHigh.setTextColor(UiUtils.androidTextColorSecondary(requireContext()))
                    itemBinding.cloudHeightMiddle.setTextColor(UiUtils.androidTextColorSecondary(requireContext()))
                    itemBinding.cloudHeightLow.setTextColor(UiUtils.color(requireContext(), R.color.colorPrimary))
                    itemBinding.cloudHeightHigh.alpha = 0.25f
                    itemBinding.cloudHeightMiddle.alpha = 0.25f
                    itemBinding.cloudHeightLow.alpha = 1f
                }
                CloudHeight.Middle -> {
                    itemBinding.cloudHeightHigh.setTextColor(UiUtils.androidTextColorSecondary(requireContext()))
                    itemBinding.cloudHeightMiddle.setTextColor(UiUtils.color(requireContext(), R.color.colorPrimary))
                    itemBinding.cloudHeightLow.setTextColor(UiUtils.androidTextColorSecondary(requireContext()))
                    itemBinding.cloudHeightHigh.alpha = 0.25f
                    itemBinding.cloudHeightMiddle.alpha = 1f
                    itemBinding.cloudHeightLow.alpha = 0.25f
                }
                CloudHeight.High -> {
                    itemBinding.cloudHeightHigh.setTextColor(UiUtils.color(requireContext(), R.color.colorPrimary))
                    itemBinding.cloudHeightMiddle.setTextColor(UiUtils.androidTextColorSecondary(requireContext()))
                    itemBinding.cloudHeightLow.setTextColor(UiUtils.androidTextColorSecondary(requireContext()))
                    itemBinding.cloudHeightHigh.alpha = 1f
                    itemBinding.cloudHeightMiddle.alpha = 0.25f
                    itemBinding.cloudHeightLow.alpha = 0.25f
                }
            }

        }

        listView.addLineSeparator()
        listView.setData(cloudRepo.getClouds().sortedByDescending { it.height })
    }
}