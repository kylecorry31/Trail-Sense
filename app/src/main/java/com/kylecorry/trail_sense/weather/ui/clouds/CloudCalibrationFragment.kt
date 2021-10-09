package com.kylecorry.trail_sense.weather.ui.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.graphics.set
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.weather.domain.clouds.AMTCloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.NRBRSkyThresholdCalculator
import com.kylecorry.trail_sense.weather.domain.clouds.SkyPixelClassification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudCalibrationFragment : BoundFragment<FragmentCloudScanBinding>() {

    private var lastBitmap: Bitmap? = null
    private var clouds: Bitmap? = null
    private var showingBitmask = false

    private var onCloudResults: (List<ClassificationResult<CloudGenus>>) -> Unit = {}
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
        binding.thresholdSeek.progress = 60
        binding.threshold.text = "60"
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
            binding.cloudImage.setImageBitmap(it)
            updateThreshold(it)
        }

        toast(getString(R.string.cloud_photo_mask_toast))
    }

    fun setOnResultsListener(listener: (List<ClassificationResult<CloudGenus>>) -> Unit) {
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
            updateThreshold(it)
        }
    }

    private fun updateThreshold(image: Bitmap){
        if (!isBound) {
            return
        }
        val thresholdCalculator = NRBRSkyThresholdCalculator()
        runInBackground {
            val threshold = withContext(Dispatchers.Default){
                thresholdCalculator.getThreshold(image)
            }

            withContext(Dispatchers.Main){
                if (isBound){
                    binding.thresholdSeek.progress = threshold
                }
                analyze(image)
            }
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
                val analyzer = AMTCloudClassifier(
                    binding.thresholdSeek.progress,
                    binding.thresholdObstacleSeek.progress
                )
                val result = withContext(Dispatchers.IO) {
                    analyzer.classify(image) { x, y, classification ->
                        when (classification) {
                            SkyPixelClassification.Sky -> clouds?.set(x, y, skyColorOverlay)
                            SkyPixelClassification.Cloud -> clouds?.set(x, y, cloudColorOverlay)
                            SkyPixelClassification.Obstacle -> clouds?.set(
                                x,
                                y,
                                excludedColorOverlay
                            )
                        }
                    }
                }

                if (isBound) {
                    withContext(Dispatchers.Main) {
                        binding.cloudImage.invalidate()
                    }
                }

                onCloudResults.invoke(result)
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