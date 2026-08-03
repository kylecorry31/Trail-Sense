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
import com.kylecorry.trail_sense.databinding.FragmentStepLengthEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.step_length.StepLengthEstimatorFactory

class FragmentStepLengthEstimation : BoundFragment<FragmentStepLengthEstimationBinding>() {

    private val estimator by lazy {
        StepLengthEstimatorFactory(requireContext()).getEstimator()
    }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private val prefs by lazy { UserPreferences(requireContext()) }

    private val units by lazy { prefs.baseDistanceUnits }

    private var isRunning = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.stepLengthBtn.setOnClickListener {
            when {
                !isRunning && estimator.hasValidReading -> {
                    prefs.pedometer.stepLength = estimator.stepLength ?: Distance.meters(0f)
                    toast(getString(R.string.saved))
                }

                !isRunning -> {
                    start()
                }

                else -> {
                    isRunning = false
                    estimator.stop(this::onStepLengthChanged)
                }
            }
        }

        binding.resetStepBtn.setOnClickListener {
            estimator.reset()
        }

        scheduleUpdates(INTERVAL_30_FPS)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStepLengthEstimationBinding {
        return FragmentStepLengthEstimationBinding.inflate(layoutInflater, container, false)
    }

    private fun onStepLengthChanged(): Boolean {
        return true
    }

    override fun onUpdate() {
        super.onUpdate()

        estimator.stepLength.let {
            binding.resetStepBtn.isVisible = !isRunning && it != null
            binding.stepLengthTitle.title.text = if (it != null) {
                formatter.formatDistance(it.convertTo(units), 2, false)
            } else {
                getString(R.string.dash)
            }
        }

        binding.stepLengthBtn.text = when {
            !isRunning && estimator.hasValidReading -> getString(R.string.save)
            !isRunning -> getString(R.string.start)
            else -> getString(R.string.stop)
        }

        binding.stepLengthDescription.text = when {
            isRunning && !estimator.hasValidReading -> getString(R.string.step_length_stand_still)
            isRunning -> getString(R.string.step_length_walk)
            else -> ""
        }
    }

    override fun onPause() {
        super.onPause()
        estimator.stop(this::onStepLengthChanged)
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            estimator.start(this::onStepLengthChanged)
        }
    }

    private fun start() {
        requestActivityRecognition { hasPermission ->
            if (hasPermission) {
                isRunning = true
                estimator.start(this::onStepLengthChanged)
            } else {
                isRunning = false
                alertNoActivityRecognitionPermission()
            }
        }
    }
}
