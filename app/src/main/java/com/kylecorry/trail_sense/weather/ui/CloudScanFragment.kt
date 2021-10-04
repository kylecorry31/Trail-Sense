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
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.maps.infrastructure.resize
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudAnalyzer
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class CloudScanFragment : BoundFragment<FragmentCloudScanBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<CloudType>
    private var currentClouds = emptyList<CloudType>()
    private var lastBitmap: Bitmap? = null
    private var clouds: Bitmap? = null
    private var showingBitmask = false

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

        listView = ListView(binding.cloudList, R.layout.list_item_cloud) { itemView, item ->
            val itemBinding = ListItemCloudBinding.bind(itemView)
            CloudListItem(item, cloudRepo).display(itemBinding)
        }

        listView.addLineSeparator()

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

        loadImage()

        binding.cloudImage.setOnClickListener {
            showingBitmask = !showingBitmask
            if (showingBitmask) {
                binding.cloudImage.setImageBitmap(clouds)
            } else {
                binding.cloudImage.setImageBitmap(lastBitmap)
            }
        }
    }

    private fun loadImage() {
        CustomUiUtils.pickImage(this) { image ->
            if (image != null) {
                binding.cloudImage.setImageBitmap(null)
                lastBitmap?.recycle()
                clouds?.recycle()
                lastBitmap = image.resize(500, 500)
                image.recycle()
                clouds = lastBitmap?.let {
                    it.copy(it.config, true)
                }
                binding.cloudImage.setImageBitmap(lastBitmap)
                showingBitmask = false
                lastBitmap?.let {
                    analyze(it)
                }
            }
        }
    }

    private fun analyze(image: Bitmap) {
        // TODO: Only queue up once
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
                if (isBound) {
                    val newClouds = observation.possibleClouds
                    if (!(newClouds.toTypedArray() contentEquals currentClouds.toTypedArray())) {
                        listView.setData(newClouds)
                        listView.scrollToPosition(0, false)
                        currentClouds = newClouds
                    }

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
        lastBitmap?.recycle()
        clouds?.recycle()
    }
}