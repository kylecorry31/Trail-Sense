package com.kylecorry.trail_sense.tools.pedometer.ui

import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useTrigger
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.step_length.StepLengthEstimatorFactory

class FragmentStepLengthEstimation :
    TrailSenseReactiveFragment(R.layout.fragment_step_length_estimation) {

    override fun update() {
        // Views
        val titleView = useView<Toolbar>(R.id.step_length_title)
        val descriptionView = useView<TextView>(R.id.step_length_description)
        val stepLengthButtonView = useView<Button>(R.id.step_length_btn)
        val resetButtonView = useView<ImageButton>(R.id.reset_step_btn)

        // Services
        val context = useAndroidContext()
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val estimator = useMemo(context) { StepLengthEstimatorFactory(context).getEstimator() }

        // State
        val (isRunning, setIsRunning) = useState(false)
        val (readingKey, refreshReading) = useTrigger()
        val stepLength = useMemo(estimator, readingKey) { estimator.stepLength }
        val hasValidReading = useMemo(estimator, readingKey) { estimator.hasValidReading }

        useEffectWithCleanup(estimator, isRunning, resetOnResume) {
            val onStepLengthChanged = {
                refreshReading()
                true
            }
            if (isRunning) {
                estimator.start(onStepLengthChanged)
            }
            return@useEffectWithCleanup {
                estimator.stop(onStepLengthChanged)
            }
        }

        // View - Buttons
        useEffect(stepLengthButtonView, estimator, prefs, isRunning, hasValidReading) {
            stepLengthButtonView.setOnClickListener {
                when {
                    !isRunning && hasValidReading -> {
                        prefs.pedometer.stepLength = estimator.stepLength ?: Distance.meters(0f)
                        toast(getString(R.string.saved))
                    }

                    !isRunning -> {
                        requestActivityRecognition { hasPermission ->
                            setIsRunning(hasPermission)
                            if (!hasPermission) {
                                alertNoActivityRecognitionPermission()
                            }
                        }
                    }

                    else -> setIsRunning(false)
                }
            }
        }

        // The estimator doesn't notify listeners when reset, so refresh manually
        useEffect(resetButtonView, estimator) {
            resetButtonView.setOnClickListener {
                estimator.reset()
                refreshReading()
            }
        }

        // View - Text
        useEffect(resetButtonView, isRunning, stepLength) {
            resetButtonView.isVisible = !isRunning && stepLength != null
        }

        useEffect(titleView, stepLength, prefs, formatter) {
            titleView.title.text = if (stepLength != null) {
                formatter.formatDistance(stepLength.convertTo(prefs.baseDistanceUnits), 2, false)
            } else {
                getString(R.string.dash)
            }
        }

        useEffect(stepLengthButtonView, isRunning, hasValidReading) {
            stepLengthButtonView.text = when {
                !isRunning && hasValidReading -> getString(R.string.save)
                !isRunning -> getString(R.string.start)
                else -> getString(R.string.stop)
            }
        }

        useEffect(descriptionView, isRunning, hasValidReading) {
            descriptionView.text = when {
                isRunning && !hasValidReading -> getString(R.string.step_length_stand_still)
                isRunning -> getString(R.string.step_length_walk)
                else -> ""
            }
        }
    }
}
