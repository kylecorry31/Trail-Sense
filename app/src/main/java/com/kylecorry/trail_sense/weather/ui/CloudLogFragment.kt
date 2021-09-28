package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudLogBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.domain.CloudObservation
import com.kylecorry.trail_sense.weather.domain.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudObservationRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class CloudLogFragment : BoundFragment<FragmentCloudLogBinding>() {

    private val repo by lazy { CloudObservationRepo.getInstance(requireContext()) }
    private val cloudService = CloudService()
    private val formatService by lazy { FormatService(requireContext()) }
    private lateinit var list: ListView<Reading<CloudObservation>>
    private lateinit var chart: CloudChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = ListView(
            binding.cloudLog,
            R.layout.list_item_plain
        ) { view, reading ->
            displayReading(ListItemPlainBinding.bind(view), reading)
        }
        list.addLineSeparator()

        chart = CloudChart(binding.chart)

        repo.getAllLive().observe(viewLifecycleOwner, {
            val clouds = it.sortedBy { it.time }
            list.setData(clouds.reversed())
            chart.plot(clouds)
            updateCloudForecast(clouds)
        })

        binding.logBtn.setOnClickListener {
            log()
        }

        binding.cloudTypeBtn.setOnClickListener {
            viewCloudTypes()
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudLogBinding {
        return FragmentCloudLogBinding.inflate(layoutInflater, container, false)
    }

    private fun updateCloudForecast(clouds: List<Reading<CloudObservation>>) {
        val tendency = cloudService.forecastClouds(clouds.filter {
            it.time >= Instant.now().minus(
                Duration.ofHours(5)
            )
        })
        binding.cloudWeather.text = when {
            tendency == 0f -> {
                "Not changing"
            }
            tendency > 0f -> {
                "Clouds increasing (${formatService.formatPercentage(tendency * 100)}} / hr)"
            }
            else -> {
                "Clouds decreasing (${formatService.formatPercentage(-tendency * 100)}} / hr)"
            }
        }
    }

    private fun log() {
        findNavController().navigate(R.id.action_cloud_log_to_cloud_scan)
    }

    private fun viewCloudTypes(){
        findNavController().navigate(R.id.action_cloud_log_to_cloud_types)
    }

    private fun displayReading(
        itemBinding: ListItemPlainBinding,
        reading: Reading<CloudObservation>
    ) {
        itemBinding.title.text =
            formatService.formatCloudCover(cloudService.classifyCloudCover(reading.value.coverage))
        itemBinding.description.text = formatService.formatPercentage(reading.value.coverage * 100)
        itemBinding.root.setOnLongClickListener {
            runInBackground {
                withContext(Dispatchers.IO) {
                    repo.delete(reading)
                }
            }
            true
        }
        // TODO: Display time
    }
}