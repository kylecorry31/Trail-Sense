package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentStrideLengthEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length.GPSStrideLengthCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FragmentStrideLengthEstimation : BoundFragment<FragmentStrideLengthEstimationBinding>() {

    private val estimator by lazy {
        GPSStrideLengthCalculator(
            SensorService(requireContext()).getGPS(),
            Pedometer(requireContext())
        )
    }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private val prefs by lazy { UserPreferences(requireContext()) }

    private val units by lazy { prefs.baseDistanceUnits }

    private var strideLength: Distance? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.strideLengthBtn.setOnClickListener {
            val state = estimator.state
            val stride = strideLength
            when {
                stride != null -> {
                    prefs.pedometer.strideLength = stride
                    toast(getString(R.string.saved))
                }
                state == GPSStrideLengthCalculator.State.Stopped -> {
                    runInBackground {
                        withContext(Dispatchers.Default) {
                            strideLength = estimator.calculate()
                        }
                    }
                }
                else -> {
                    estimator.stop()
                }
            }
        }

        binding.resetStrideBtn.setOnClickListener {
            strideLength = null
        }

        scheduleUpdates(INTERVAL_30_FPS)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStrideLengthEstimationBinding {
        return FragmentStrideLengthEstimationBinding.inflate(layoutInflater, container, false)
    }

    override fun onUpdate() {
        super.onUpdate()

        strideLength.let {
            binding.resetStrideBtn.isVisible = it != null
            binding.strideLengthTitle.title.text = if (it != null) {
                formatter.formatDistance(it.convertTo(units), 2, false)
            } else {
                getString(R.string.dash)
            }
        }

        val state = estimator.state

        binding.strideLengthBtn.text = when {
            strideLength != null -> getString(R.string.save)
            state == GPSStrideLengthCalculator.State.Stopped -> getString(R.string.start)
            else -> getString(R.string.stop)
        }

        binding.strideLengthDescription.text = when (state) {
            GPSStrideLengthCalculator.State.Stopped -> ""
            GPSStrideLengthCalculator.State.Starting -> getString(R.string.stride_length_stand_still)
            GPSStrideLengthCalculator.State.Started -> getString(R.string.stride_length_walk)
            GPSStrideLengthCalculator.State.Stopping -> getString(R.string.stride_length_stand_still)
        }

    }

    override fun onPause() {
        super.onPause()
        estimator.stop(true)
    }
}