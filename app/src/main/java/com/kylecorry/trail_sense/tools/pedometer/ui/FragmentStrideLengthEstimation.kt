package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentStrideLengthEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.StrideLengthEstimatorFactory

class FragmentStrideLengthEstimation : BoundFragment<FragmentStrideLengthEstimationBinding>() {

    private val estimator by lazy {
        StrideLengthEstimatorFactory(requireContext()).getEstimator()
    }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private val prefs by lazy { UserPreferences(requireContext()) }

    private val units by lazy { prefs.baseDistanceUnits }

    private var isRunning = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.strideLengthBtn.setOnClickListener {
            when {
                !isRunning && estimator.hasValidReading -> {
                    prefs.pedometer.strideLength = estimator.strideLength ?: Distance.meters(0f)
                    toast(getString(R.string.saved))
                }
                !isRunning -> {
                    start()
                }
                isRunning -> {
                    isRunning = false
                    estimator.stop(this::onStrideLengthChanged)
                }
            }
        }

        binding.resetStrideBtn.setOnClickListener {
            estimator.reset()
        }

        scheduleUpdates(INTERVAL_30_FPS)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStrideLengthEstimationBinding {
        return FragmentStrideLengthEstimationBinding.inflate(layoutInflater, container, false)
    }

    private fun onStrideLengthChanged(): Boolean {
        return true
    }

    override fun onUpdate() {
        super.onUpdate()

        estimator.strideLength.let {
            binding.resetStrideBtn.isVisible = !isRunning && it != null
            binding.strideLengthTitle.title.text = if (it != null) {
                formatter.formatDistance(it.convertTo(units), 2, false)
            } else {
                getString(R.string.dash)
            }
        }

        binding.strideLengthBtn.text = when {
            !isRunning && estimator.hasValidReading -> getString(R.string.save)
            !isRunning -> getString(R.string.start)
            else -> getString(R.string.stop)
        }

        binding.strideLengthDescription.text = when {
            isRunning && !estimator.hasValidReading -> getString(R.string.stride_length_stand_still)
            isRunning -> getString(R.string.stride_length_walk)
            else -> ""
        }
    }

    override fun onPause() {
        super.onPause()
        estimator.stop(this::onStrideLengthChanged)
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            estimator.start(this::onStrideLengthChanged)
        }
    }

    private fun start(){
        requestActivityRecognition { hasPermission ->
            if (hasPermission){
                isRunning = true
                estimator.start(this::onStrideLengthChanged)
            } else {
                isRunning = false
                alertNoActivityRecognitionPermission()
            }
        }
    }
}