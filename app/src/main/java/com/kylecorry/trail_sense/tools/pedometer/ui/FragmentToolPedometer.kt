package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.topics.generic.asLiveData
import com.kylecorry.andromeda.core.topics.generic.getOrNull
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
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
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter
import com.kylecorry.trail_sense.tools.pedometer.domain.PedometerSession
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.PedometerSessionRepo
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.google.android.material.tabs.TabLayoutMediator
import java.time.Instant
import java.time.LocalDate

class FragmentToolPedometer : BoundFragment<FragmentToolPedometerBinding>() {

    private val pedometer by lazy { PedometerSubsystem.getInstance(requireContext()) }
    private val counter by lazy { StepCounter(PreferencesSubsystem.getInstance(requireContext()).preferences) }
    private val paceCalculator by lazy { StrideLengthPaceCalculator(prefs.pedometer.strideLength) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sessionRepo by lazy { PedometerSessionRepo.getInstance(requireContext()) }

    private val historyPages = mutableListOf<FragmentPedometerPeriodHistory>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPedometerBinding {
        return FragmentToolPedometerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset button in the toolbar left position
        binding.pedometerTitle.leftButton.setOnClickListener {
            Alerts.dialog(requireContext(), getString(R.string.reset_distance_title)) {
                if (!it) {
                    saveCurrentSession()
                    counter.reset()
                    refreshHistoryTabs()
                }
            }
        }

        // Distance alert in toolbar right position
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

        setupHistoryTabs()

        pedometer.state.replay().asLiveData().observe(viewLifecycleOwner) { updateStatusBar() }
        scheduleUpdates(500L)
    }

    private fun setupHistoryTabs() {
        historyPages.clear()
        historyPages.addAll(
            FragmentPedometerPeriodHistory.PeriodType.entries.map {
                FragmentPedometerPeriodHistory.newInstance(it)
            }
        )

        val tabNames = listOf(
            getString(R.string.day),
            getString(R.string.week),
            getString(R.string.month),
            getString(R.string.year)
        )

        binding.historyViewpager.adapter = CustomViewPagerAdapter(this, historyPages)
        binding.historyViewpager.isUserInputEnabled = true
        // Keep all 4 tabs in memory so switching never triggers a visible reload/flicker
        binding.historyViewpager.offscreenPageLimit = 3

        TabLayoutMediator(binding.historyTabs, binding.historyViewpager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()
    }

    private fun refreshHistoryTabs() {
        for (page in historyPages) {
            page.refresh()
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        val steps = counter.steps
        val distance = getDistance(steps)
        val lastReset = counter.startTime?.toZonedDateTime()

        CustomUiUtils.setButtonState(
            binding.pedometerTitle.rightButton,
            prefs.pedometer.alertDistance != null
        )

        val stepsText = DecimalFormatter.format(steps, 0)
        val distText = formatService.formatDistance(
            distance, Units.getDecimalPlaces(distance.units), false
        )
        binding.pedometerTitle.title.text = "$stepsText ${getString(R.string.steps)} · $distText"

        if (lastReset != null) {
            val dateString = if (lastReset.toLocalDate() == LocalDate.now()) {
                formatService.formatTime(lastReset.toLocalTime(), false)
            } else {
                formatService.formatRelativeDate(lastReset.toLocalDate())
            }
            binding.pedometerTitle.subtitle.text = getString(R.string.since_time, dateString)
        }

        binding.pedometerTitle.subtitle.isVisible = lastReset != null
    }

    private fun updateStatusBar() {
        binding.pedometerPlayBar.setState(
            pedometer.state.getOrNull() ?: FeatureState.Off
        )
    }

    private fun getDistance(steps: Long): Distance {
        val distance = paceCalculator.distance(steps)
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }

    private fun saveCurrentSession() {
        val steps = counter.steps
        val startTime = counter.startTime ?: return
        if (steps <= 0) return

        val distance = paceCalculator.distance(steps).meters().value
        val session = PedometerSession(
            0,
            startTime,
            Instant.now(),
            steps,
            distance
        )
        inBackground {
            sessionRepo.add(session)
        }
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
