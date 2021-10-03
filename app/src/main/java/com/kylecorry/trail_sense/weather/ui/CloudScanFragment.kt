package com.kylecorry.trail_sense.weather.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.math.SolMath.clamp
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudScanBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudSensor
import kotlin.math.roundToInt

class CloudScanFragment : BoundFragment<FragmentCloudScanBinding>() {

    private val cloudSensor by lazy { CloudSensor(requireContext(), this) }
    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<CloudType>
    private var currentClouds = emptyList<CloudType>()

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

        listView = ListView(binding.cloudList, R.layout.list_item_cloud) { itemView, item ->
            val itemBinding = ListItemCloudBinding.bind(itemView)
            CloudListItem(item, cloudRepo).display(itemBinding)
        }

        listView.addLineSeparator()

        var lastBitmap: Bitmap? = null
        cloudSensor.asLiveData().observe(viewLifecycleOwner, {
            cloudSensor.observation?.let { observation ->
                val newClouds = observation.possibleClouds
                if (!(newClouds.toTypedArray() contentEquals currentClouds.toTypedArray())) {
                    listView.setData(newClouds)
                    listView.scrollToPosition(0, false)
                    currentClouds = newClouds
                }

                // This is for testing only
                binding.features.text = "Cover: ${(observation.cover * 100).roundToInt()}  Contrast: ${(observation.contrast * 100).roundToInt()}  Luminance: ${(observation.luminance * 100).roundToInt()}"
            }

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
    }

    override fun onDestroy() {
        super.onDestroy()
        cloudSensor.destroy()
    }
}