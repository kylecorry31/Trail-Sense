package com.kylecorry.trail_sense.weather.ui.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.weather.domain.clouds.classification.TextureCloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.classification.ICloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.CloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.NRBRSkyThresholdCalculator
import com.kylecorry.trail_sense.weather.domain.clouds.mask.OverlayCloudMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudCalibrationFragment : BoundFragment<FragmentCloudScanBinding>() {

    private var lastBitmap: Bitmap? = null
    private var clouds: Bitmap? = null
    private var showingBitmask = false

    private var onClassifierChanged: (ICloudClassifier) -> Unit = {}
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
        binding.thresholdObstacleSeek.progress = 25
        binding.thresholdObstacle.text = "25"

        binding.thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.threshold.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                lastBitmap?.let {
                    mask(it)
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
                    mask(it)
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

    fun setOnClassifierChangedListener(listener: (ICloudClassifier) -> Unit) {
        onClassifierChanged = listener
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

    private fun updateThreshold(image: Bitmap) {
        if (!isBound) {
            return
        }
        val thresholdCalculator = NRBRSkyThresholdCalculator()
        inBackground {
            val threshold = withContext(Dispatchers.Default) {
                thresholdCalculator.getThreshold(image)
            }

            onMain {
                if (isBound) {
                    binding.thresholdSeek.progress = threshold
                    binding.thresholdObstacleSeek.progress = 25
                }
                mask(image)
            }
        }
    }

    private fun mask(image: Bitmap) {
        if (!isBound) {
            return
        }
        inBackground {
            runner.cancelPreviousThenRun {
                val cloudColor = Color.WHITE
                val obstacleColor = AppColor.Red.color
                val skyColor = AppColor.Blue.color

                val pixelClassifier = CloudPixelClassifier.default(
                    binding.thresholdSeek.progress,
                    binding.thresholdObstacleSeek.progress
                )
                val mask = OverlayCloudMask(pixelClassifier, cloudColor, skyColor, obstacleColor)
//                val mask = DebugCloudMask()
                val classifier = TextureCloudClassifier(pixelClassifier)

                clouds = onIO {
                    mask.mask(image, clouds)
                }

                if (isBound) {
                    onMain {
                        binding.cloudImage.invalidate()
                    }
                }

                onClassifierChanged.invoke(classifier)
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