package com.kylecorry.trail_sense.tools.pedometer.ui

import android.widget.Button
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.luna.topics.generic.replay
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.DataPointView
import com.kylecorry.trail_sense.shared.views.PlayBarView
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import java.time.LocalDate

class FragmentToolPedometer : TrailSenseReactiveFragment(R.layout.fragment_tool_pedometer, 500L) {

    override fun update() {
        // Views
        val resetButtonView = useView<Button>(R.id.reset_btn)
        val playBarView = useView<PlayBarView>(R.id.pedometer_play_bar)
        val titleView = useView<Toolbar>(R.id.pedometer_title)
        val stepsView = useView<DataPointView>(R.id.pedometer_steps)
        val speedView = useView<DataPointView>(R.id.pedometer_speed)
        val averageSpeedView = useView<DataPointView>(R.id.pedometer_average_speed)

        // Services
        val context = useAndroidContext()
        val pedometer = useService<PedometerSubsystem>()
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val counter = useMemo(context) {
            StepCounter(PreferencesSubsystem.getInstance(context).preferences)
        }
        val paceCalculator = useMemo(prefs.pedometer.strideLength) {
            StrideLengthPaceCalculator(prefs.pedometer.strideLength)
        }
        val averageSpeedometer = useMemo(counter, paceCalculator) {
            AveragePaceSpeedometer(counter, paceCalculator)
        }
        val instantSpeedometer = useMemo(context, paceCalculator) {
            CurrentPaceSpeedometer(Pedometer(context), paceCalculator)
        }

        // State
        val stepsTopic = useMemo(pedometer) { pedometer.steps.replay() }
        val stateTopic = useMemo(pedometer) { pedometer.state.replay() }
        val steps = useTopic(stepsTopic, counter.steps) { it }
        val state = useTopic(
            stateTopic,
            pedometer.state.getOrNull() ?: FeatureState.Off
        ) { it }
        val averageSpeed = useTopic(averageSpeedometer, SpeedReading()) {
            SpeedReading(averageSpeedometer.speed, averageSpeedometer.hasValidReading)
        }
        val currentSpeed = useTopic(instantSpeedometer, SpeedReading()) {
            SpeedReading(instantSpeedometer.speed, instantSpeedometer.hasValidReading)
        }
        val distance = useMemo(steps, paceCalculator, prefs) {
            val distance = paceCalculator.distance(steps)
            distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        }
        val lastReset = counter.startTime?.toZonedDateTime()
        val hasAlertDistance = prefs.pedometer.alertDistance != null

        // Effects
        useEffect(resetButtonView, counter, context) {
            resetButtonView.setOnClickListener {
                Alerts.dialog(context, getString(R.string.reset_distance_title)) {
                    if (!it) {
                        counter.reset()
                    }
                }
            }
        }

        useEffect(playBarView, state, pedometer) {
            playBarView.setOnPlayButtonClickListener {
                when (pedometer.state.getOrNull()) {
                    FeatureState.On -> pedometer.disable()
                    FeatureState.Off -> startStepCounter(pedometer)
                    else -> {
                        if (pedometer.isDisabledDueToPermissions()) {
                            startStepCounter(pedometer)
                        }
                    }
                }
            }
        }

        useEffect(titleView, prefs, formatter, hasAlertDistance) {
            titleView.rightButton.setOnClickListener {
                showAlertDistanceDialog(prefs, formatter)
            }
        }

        useEffect(titleView, hasAlertDistance) {
            titleView.rightButton.isChecked = hasAlertDistance
        }

        useEffect(playBarView, state) {
            playBarView.setState(state)
        }

        useEffect(titleView, lastReset) {
            if (lastReset != null) {
                val dateString = if (lastReset.toLocalDate() == LocalDate.now()) {
                    formatter.formatTime(lastReset.toLocalTime(), false)
                } else {
                    formatter.formatRelativeDate(lastReset.toLocalDate())
                }
                titleView.subtitle.text = getString(R.string.since_time, dateString)
            }

            titleView.subtitle.isVisible = lastReset != null
        }

        useEffect(stepsView, steps) {
            stepsView.title = DecimalFormatter.format(steps, 0)
        }

        useEffect(titleView, distance) {
            titleView.title.text = formatter.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )
        }

        useEffect(averageSpeedView, averageSpeed) {
            averageSpeedView.title = if (averageSpeed.hasValidReading) {
                formatter.formatSpeed(averageSpeed.speed.value)
            } else {
                getString(R.string.dash)
            }
        }

        useEffect(speedView, currentSpeed) {
            speedView.title = if (currentSpeed.hasValidReading) {
                formatter.formatSpeed(currentSpeed.speed.value)
            } else {
                getString(R.string.dash)
            }
        }
    }

    private fun showAlertDistanceDialog(
        prefs: UserPreferences,
        formatService: FormatService
    ) {
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

    private fun startStepCounter(pedometer: PedometerSubsystem) {
        requestActivityRecognition { hasPermission ->
            if (hasPermission) {
                pedometer.enable()
            } else {
                pedometer.disable()
                alertNoActivityRecognitionPermission()
            }
        }
    }

    private data class SpeedReading(
        val speed: Speed = ZERO_SPEED,
        val hasValidReading: Boolean = false
    )

}
