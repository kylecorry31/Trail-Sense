package com.kylecorry.trail_sense.weather.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.graphics.set
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudAnalyzer
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudObservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class CloudCalibrationFragment : BoundFragment<FragmentCloudScanBinding>() {

    private var lastBitmap: Bitmap? = null
    private var clouds: Bitmap? = null
    private var showingBitmask = false

    private var onCloudResults: (CloudObservation) -> Unit = {}
    private var onDone: () -> Unit = {}

    private val runner = ControlledRunner<Unit>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudScanBinding {
        return FragmentCloudScanBinding.inflate(layoutInflater, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.thresholdSeek.max = 100
        binding.thresholdObstacleSeek.max = 100
        binding.thresholdSeek.progress = 50
        binding.threshold.text = "50"
        binding.thresholdObstacleSeek.progress = 0
        binding.thresholdObstacle.text = "0"

        binding.thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.threshold.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                lastBitmap?.let {
                    analyze(it)
                }
            }
        })

        binding.thresholdObstacleSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.thresholdObstacle.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                lastBitmap?.let {
                    analyze(it)
                }
            }
        })

        binding.cloudImage.setOnClickListener {
            showingBitmask = !showingBitmask
            if (showingBitmask) {
                binding.cloudImage.setImageBitmap(clouds)
            } else {
                binding.cloudImage.setImageBitmap(lastBitmap)
            }
        }

        binding.doneBtn.setOnClickListener {
            onDone.invoke()
        }

        lastBitmap?.let {
            setImage(it)
        }
    }

    fun setOnResultsListener(listener: (CloudObservation) -> Unit) {
        onCloudResults = listener
    }

    fun setOnDoneListener(listener: () -> Unit) {
        onDone = listener
    }

    fun setImage(image: Bitmap) {
        if (isBound) {
            binding.cloudImage.setImageBitmap(null)
        }
        clouds?.recycle()
        lastBitmap = image
        clouds = lastBitmap?.let {
            it.copy(it.config, true)
        }
        if (isBound) {
            binding.cloudImage.setImageBitmap(lastBitmap)
        }
        showingBitmask = false
        lastBitmap?.let {
            analyze(it)
        }
    }

    private fun analyze(image: Bitmap) {
        if (!isBound) {
            return
        }
        runInBackground {
            runner.cancelPreviousThenRun {
                val cloudColorOverlay = Color.WHITE
                val excludedColorOverlay = AppColor.Red.color
                val skyColorOverlay = AppColor.Blue.color
                val analyzer = CloudAnalyzer(
                    binding.thresholdSeek.progress,
                    binding.thresholdObstacleSeek.progress,
                    skyColorOverlay,
                    excludedColorOverlay,
                    cloudColorOverlay
                )
                val observation = withContext(Dispatchers.IO) {
                    analyzer.getClouds(image) { x, y, pixel ->
                        clouds?.set(x, y, pixel)
                    }
                }
                onCloudResults.invoke(observation)
                if (isBound) {
                    // This is for testing only
                    binding.features.text =
                        "Cover: ${(observation.cover * 100).roundToInt()}  Contrast: ${(observation.contrast * 100).roundToInt()}  Luminance: ${(observation.luminance * 100).roundToInt()}"
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        runner.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        clouds?.recycle()
    }
}