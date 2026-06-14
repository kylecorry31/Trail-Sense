package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.asLiveData
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.luna.topics.generic.replay
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolPedometerBinding
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class FragmentToolPedometer : BoundFragment<FragmentToolPedometerBinding>() {

    private val pedometer by lazy { PedometerSubsystem.getInstance(requireContext()) }
    private val stepTrackerService by lazy { getAppService<StepTrackerService>() }
    private val paceCalculator by lazy { StrideLengthPaceCalculator(prefs.pedometer.strideLength) }
    private val averageSpeedometer by lazy {
        AveragePaceSpeedometer(stepTrackerService, paceCalculator)
    }
    private val instantSpeedometer by lazy {
        CurrentPaceSpeedometer(Pedometer(requireContext()), paceCalculator)
    }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var hourlyStepsChart: HourlyStepsChart? = null

    private var steps by state(0L)
    private var lastResetTime by state<Instant?>(null)
    private var selectedDate by state(LocalDate.now())
    private var hourlySteps by state(emptyList<HourlyStepCount>())
    private var selectedHourlySteps by state<HourlyStepCount?>(null)

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPedometerBinding {
        return FragmentToolPedometerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hourlyStepsChart = HourlyStepsChart(binding.hourlyStepsChart) {
            selectedHourlySteps = it
        }
        setupResetButton()
        setupPedometerPlayBar()
        setupDistanceAlertButton()
        setupHourlyStepsDatePicker()

        observe(averageSpeedometer) { onUpdate() }

        observe(instantSpeedometer) { onUpdate() }

        pedometer.state.replay().asLiveData().observe(viewLifecycleOwner) { updateStatusBar() }

        // TODO: Use pedometer subsystem topics
        scheduleUpdates(500L)
    }

    private fun setupResetButton() {
        binding.resetBtn.setOnClickListener {
            Alerts.dialog(requireContext(), getString(R.string.reset_distance_title)) {
                if (!it) {
                    inBackground {
                        stepTrackerService.startNewStepTrackingPeriod()
                    }
                }
            }
        }
    }

    private fun setupPedometerPlayBar() {
        binding.pedometerPlayBar.setOnPlayButtonClickListener {
            when (pedometer.state.getOrNull()) {
                FeatureState.On -> pedometer.disable()
                FeatureState.Off -> startStepCounter()
                else -> {
                    if (pedometer.isDisabledDueToPermissions()) {
                        startStepCounter()
                    }
                }
            }
        }
    }

    private fun setupDistanceAlertButton() {
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
    }

    private fun setupHourlyStepsDatePicker() {
        binding.hourlyStepsDate.date = selectedDate
        binding.hourlyStepsDate.setOnDateChangeListener {
            selectedDate = it
            inBackground {
                updateHourlySteps()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Tools.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, ::updateSteps)
        inBackground { updateSteps(null) }
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, ::updateSteps)
    }

    private suspend fun updateSteps(data: Bundle?) {
        val trackingPeriod = stepTrackerService.getOpenStepTrackingPeriod() ?: return
        steps = data?.getLong(PedometerToolRegistration.BROADCAST_PARAM_STEPS) ?: trackingPeriod.steps
        lastResetTime = trackingPeriod.startTime
        updateHourlySteps()
    }

    private suspend fun updateHourlySteps() {
        val date = selectedDate
        val updatedHourlySteps = stepTrackerService.getHourlyStepCounts(date)
        hourlySteps = updatedHourlySteps
        selectedHourlySteps = selectedHourlySteps?.let { selected ->
            updatedHourlySteps.firstOrNull { it.startTime == selected.startTime }
        }
        onMain {
            binding.hourlyStepsDate.date = date
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        val distance = getDistance(steps)
        val lastReset = lastResetTime?.toZonedDateTime()

        binding.pedometerTitle.rightButton.isChecked = prefs.pedometer.alertDistance != null

        if (lastReset != null) {
            val dateString = if (lastReset.toLocalDate() == LocalDate.now()) {
                formatService.formatTime(lastReset.toLocalTime(), false)
            } else {
                formatService.formatRelativeDate(lastReset.toLocalDate())
            }
            binding.currentSessionTime.text = getString(
                R.string.dash_separated_pair,
                dateString,
                getString(R.string.now)
            )
        }

        binding.pedometerSteps.title = DecimalFormatter.format(steps, 0)
        val formattedDistance = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
        binding.pedometerDistance.title = formattedDistance

        binding.currentSessionTime.isVisible = lastReset != null

        binding.pedometerTitle.title.text = getString(R.string.pedometer)

        updateAverageSpeed()
        updateCurrentSpeed()
        updateHourlyStepsChart()
    }

    private fun updateStatusBar() {
        binding.pedometerPlayBar.setState(
            pedometer.state.getOrNull() ?: FeatureState.Off
        )
    }

    private fun updateAverageSpeed() {
        val speed = averageSpeedometer.speed
        binding.pedometerAverageSpeed.title = if (averageSpeedometer.hasValidReading) {
            formatService.formatSpeed(speed.value)
        } else {
            getString(R.string.dash)
        }
    }

    private fun updateCurrentSpeed() {
        val speed = instantSpeedometer.speed
        binding.pedometerSpeed.title = if (instantSpeedometer.hasValidReading) {
            formatService.formatSpeed(speed.value)
        } else {
            getString(R.string.dash)
        }
    }

    private fun getDistance(steps: Long): Distance {
        val distance = paceCalculator.distance(steps)
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }

    private fun updateHourlyStepsChart() {
        val roundedDailySteps = hourlySteps.sumOf { it.steps }
        val dailyDistance = getDistance(roundedDailySteps)
        binding.hourlyStepsTotalSteps.title = DecimalFormatter.format(roundedDailySteps, 0)
        binding.hourlyStepsTotalDistance.title = formatService.formatDistance(
            dailyDistance,
            Units.getDecimalPlaces(dailyDistance.units),
            false
        )
        hourlyStepsChart?.plot(hourlySteps)
        hourlyStepsChart?.highlight(selectedHourlySteps)
        updateHourlyStepsDetails()
    }

    private fun updateHourlyStepsDetails() {
        val hourlySteps = selectedHourlySteps
        binding.hourlyStepsDetails.isVisible = hourlySteps != null
        binding.hourlyStepsDetailsGrid.isVisible = hourlySteps != null
        hourlySteps ?: return

        val steps = hourlySteps.steps
        val distance = getDistance(steps)
        binding.hourlyStepsDetails.text = getString(
            R.string.dash_separated_pair,
            formatService.formatTime(hourlySteps.startTime, false),
            formatService.formatTime(hourlySteps.endTime, false)
        )
        binding.hourlyStepsDetailsSteps.title = DecimalFormatter.format(steps, 0)
        binding.hourlyStepsDetailsDistance.title = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
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
