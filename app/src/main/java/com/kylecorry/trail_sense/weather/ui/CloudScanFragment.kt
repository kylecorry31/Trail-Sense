package com.kylecorry.trail_sense.weather.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.math.SolMath.clamp
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.weather.domain.clouds.CloudObservation
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudSensor
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudObservationRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class CloudScanFragment : BoundFragment<FragmentCloudScanBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val cloudService = CloudService()
    private val cloudSensor by lazy { CloudSensor(requireContext(), this) }
    private val cloudRepo by lazy { CloudObservationRepo.getInstance(requireContext()) }

    private val cloudImageScaleListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val zoom = clamp(cloudSensor.zoom * detector.scaleFactor, 1f, 2f)
                cloudSensor.zoom = zoom
                return true
            }
        }

    private val cloudImageGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            cloudSensor.bitmask = !cloudSensor.bitmask
            return super.onSingleTapConfirmed(e)
        }
    }

    private val mScaleDetector by lazy {
        ScaleGestureDetector(
            requireContext(),
            cloudImageScaleListener
        )
    }
    private val mGestureDetector by lazy {
        GestureDetector(
            requireContext(),
            cloudImageGestureListener
        )
    }

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
        binding.thresholdSeek.progress = cloudSensor.skyDetectionSensitivity
        binding.threshold.text = cloudSensor.skyDetectionSensitivity.toString()
        binding.thresholdObstacleSeek.progress = cloudSensor.obstacleRemovalSensitivity
        binding.thresholdObstacle.text = cloudSensor.obstacleRemovalSensitivity.toString()

        var lastBitmap: Bitmap? = null
        cloudSensor.asLiveData().observe(viewLifecycleOwner, {
            binding.coverage.text =
                formatService.formatPercentage(cloudSensor.cover * 100) + "\n" +
                        formatService.formatCloudCover(cloudService.classifyCloudCover(cloudSensor.cover))

            binding.luminance.text =
                cloudSensor.cloudType?.toString() + "\n" + formatService.formatPercentage(100 * cloudSensor.luminance)
            cloudSensor.clouds?.let {
                if (lastBitmap != it) {
                    binding.cloudImage.setImageBitmap(it)
                    lastBitmap = it
                }
                binding.cloudImage.invalidate()
            }
        })

        binding.thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cloudSensor.skyDetectionSensitivity = progress
                binding.threshold.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.thresholdObstacleSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cloudSensor.obstacleRemovalSensitivity = progress
                binding.thresholdObstacle.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.cloudImage.setOnTouchListener { _, event ->
            mScaleDetector.onTouchEvent(event)
            mGestureDetector.onTouchEvent(event)
            true
        }

        binding.recordBtn.setOnClickListener {
            record()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cloudImage.setImageBitmap(null)
        cloudSensor.destroy()
    }

    private fun record() {
        val reading = Reading(CloudObservation(0, cloudSensor.cover), Instant.now())
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