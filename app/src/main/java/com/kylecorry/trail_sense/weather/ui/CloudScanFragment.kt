package com.kylecorry.trail_sense.weather.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudCoverageSensor

class CloudScanFragment : BoundFragment<FragmentCloudScanBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val cloudSensor by lazy { CloudCoverageSensor(requireContext(), this) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudScanBinding {
        return FragmentCloudScanBinding.inflate(layoutInflater, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.thresholdSeek.max = 100
        binding.thresholdSeek.progress = cloudSensor.skyThreshold
        binding.threshold.text = cloudSensor.skyThreshold.toString()

        var lastBitmap: Bitmap? = null
        cloudSensor.asLiveData().observe(viewLifecycleOwner, {
            binding.coverage.text = formatService.formatPercentage(cloudSensor.coverage * 100)
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
    }

    override fun onPause() {
        super.onPause()
        binding.cloudImage.setImageBitmap(null)
    }

}