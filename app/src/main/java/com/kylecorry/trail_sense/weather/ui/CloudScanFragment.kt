package com.kylecorry.trail_sense.weather.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.domain.CloudObservation
import com.kylecorry.trail_sense.weather.domain.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudCoverageSensor
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudObservationRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class CloudScanFragment : BoundFragment<FragmentCloudScanBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val cloudService = CloudService()
    private val cloudSensor by lazy { CloudCoverageSensor(requireContext(), this) }
    private val cloudRepo by lazy { CloudObservationRepo.getInstance(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudScanBinding {
        return FragmentCloudScanBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.thresholdSeek.max = 100
        binding.zoomSeek.max = 100
        binding.thresholdSeek.progress = cloudSensor.skyThreshold
        binding.threshold.text = cloudSensor.skyThreshold.toString()

        var lastBitmap: Bitmap? = null
        cloudSensor.asLiveData().observe(viewLifecycleOwner, {
            binding.coverage.text = formatService.formatPercentage(cloudSensor.coverage * 100)
            binding.coverageDescription.text =
                formatService.formatCloudCover(cloudService.classifyCloudCover(cloudSensor.coverage))
            cloudSensor.clouds?.let {
                if (lastBitmap != it) {
                    binding.cloudImage.setImageBitmap(it)
                    lastBitmap = it
                }
                binding.cloudImage.invalidate()
            }
        })

        binding.cloudImage.setOnClickListener {
            cloudSensor.bitmask = !cloudSensor.bitmask
        }

        binding.thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cloudSensor.skyThreshold = progress
                binding.threshold.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        // TODO: Replace with pinch to zoom
        binding.zoomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoom = progress / 100f
                cloudSensor.setZoom(zoom)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.recordBtn.setOnClickListener {
            record()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.cloudImage.setImageBitmap(null)
    }

    private fun record() {
        val reading = Reading(CloudObservation(0, cloudSensor.coverage), Instant.now())
        runInBackground {
            withContext(Dispatchers.IO) {
                cloudRepo.add(reading)
            }
            withContext(Dispatchers.Main) {
                findNavController().navigateUp()
            }
        }
    }

}