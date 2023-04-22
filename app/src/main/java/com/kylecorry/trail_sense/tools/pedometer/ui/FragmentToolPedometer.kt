package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.topics.generic.asLiveData
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolPedometerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.getOrNull
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import java.time.LocalDate

class FragmentToolPedometer : BoundFragment<FragmentToolPedometerBinding>() {

    private val pedometer by lazy { PedometerSubsystem.getInstance(requireContext()) }
    private val counter by lazy { StepCounter(PreferencesSubsystem.getInstance(requireContext()).preferences) }
    private val paceCalculator by lazy { StrideLengthPaceCalculator(prefs.pedometer.strideLength) }
    private val averageSpeedometer by lazy {
        AveragePaceSpeedometer(counter, paceCalculator)
    }
    private val instantSpeedometer by lazy {
        CurrentPaceSpeedometer(Pedometer(requireContext()), paceCalculator)
    }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPedometerBinding {
        return FragmentToolPedometerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.resetBtn.setOnClickListener {
            Alerts.dialog(requireContext(), getString(R.string.reset_distance_title)) {
                if (!it) {
                    counter.reset()
                }
            }
        }

        binding.pedometerPlayBar.setOnPlayButtonClickListener {
            when (pedometer.state.getOrNull()) {
                FeatureState.On -> pedometer.disable()
                FeatureState.Off -> startStepCounter()
                else -> {}
            }
        }

        binding.pedometerTitle.rightButton.setOnClickListener {
            val alertDistance = prefs.pedometer.alertDistance
            if (alertDistance == null) {
                val units = formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
                CustomUiUtils.pickDistance(
                    requireContext(),
                    units,
                    title = getString(R.string.distance_alert),
                ) { distance, _ ->
                    if (distance != null) {
                        prefs.pedometer.alertDistance = distance
                    }
                }
            } else {
                val distance =
                    alertDistance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.distance_alert),
                    getString(
                        R.string.remove_distance_alert, formatService.formatDistance(
                            distance,
                            Units.getDecimalPlaces(distance.units),
                            false
                        )
                    )
                ) {
                    if (!it) {
                        prefs.pedometer.alertDistance = null
                    }
                }
            }
        }

        observe(averageSpeedometer) { onUpdate() }

        observe(instantSpeedometer) { onUpdate() }

        pedometer.state.replay().asLiveData().observe(viewLifecycleOwner) { updateStatusBar() }

        // TODO: Use pedometer subsystem topics
        scheduleUpdates(INTERVAL_30_FPS)
    }

    override fun onUpdate() {
        super.onUpdate()
        val distance = getDistance(counter.steps)
        val lastReset = counter.startTime?.toZonedDateTime()

        CustomUiUtils.setButtonState(
            binding.pedometerTitle.rightButton,
            prefs.pedometer.alertDistance != null
        )

        if (lastReset != null) {
            val dateString = if (lastReset.toLocalDate() == LocalDate.now()) {
                formatService.formatTime(lastReset.toLocalTime(), false)
            } else {
                formatService.formatRelativeDate(lastReset.toLocalDate())
            }
            binding.pedometerTitle.subtitle.text = getString(R.string.since_time, dateString)
        }

        binding.pedometerSteps.title = DecimalFormatter.format(counter.steps, 0)

        binding.pedometerTitle.subtitle.isVisible = lastReset != null

        binding.pedometerTitle.title.text = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )

        updateAverageSpeed()
        updateCurrentSpeed()
    }

    private fun updateStatusBar() {
        binding.pedometerPlayBar.setState(
            pedometer.state.getOrNull() ?: FeatureState.Off
        )
    }

    private fun updateAverageSpeed() {
        val speed = averageSpeedometer.speed
        binding.pedometerAverageSpeed.title = if (averageSpeedometer.hasValidReading) {
            formatService.formatSpeed(speed.speed)
        } else {
            getString(R.string.dash)
        }
    }

    private fun updateCurrentSpeed() {
        val speed = instantSpeedometer.speed
        binding.pedometerSpeed.title = if (averageSpeedometer.hasValidReading) {
            formatService.formatSpeed(speed.speed)
        } else {
            getString(R.string.dash)
        }
    }

    private fun getDistance(steps: Long): Distance {
        val distance = paceCalculator.distance(steps)
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }

    private fun startStepCounter() {
        requestActivityRecognition { hasPermission ->
            if (hasPermission) {
                pedometer.enable()
            } else {
                pedometer.disable()
                alertNoActivityRecognitionPermission()
            }
        }
    }

}