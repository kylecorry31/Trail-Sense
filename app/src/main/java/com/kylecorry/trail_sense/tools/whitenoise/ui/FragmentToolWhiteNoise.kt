package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.core.view.isVisible
import com.google.android.material.materialswitch.MaterialSwitch
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.DurationInputView
import com.kylecorry.trail_sense.shared.views.TileButton
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.SleepSound
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import java.time.Duration
import java.time.Instant

class FragmentToolWhiteNoise :
    TrailSenseReactiveFragment(R.layout.fragment_tool_white_noise, INTERVAL_30_FPS) {

    override fun update() {
        // Views
        val whiteNoiseButtonView = useView<TileButton>(R.id.white_noise_btn)
        val sleepTimerSwitchView = useView<MaterialSwitch>(R.id.sleep_timer_switch)
        val sleepTimerPickerView = useView<DurationInputView>(R.id.sleep_timer_picker)

        // Services
        val cache = useService<PreferencesSubsystem>().preferences
        val context = useAndroidContext()

        // Effects
        useEffect(whiteNoiseButtonView, WhiteNoiseService.isRunning) {
            whiteNoiseButtonView.setState(WhiteNoiseService.isRunning)
        }

        useEffect(context, sleepTimerSwitchView, sleepTimerPickerView) {
            val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
            sleepTimerSwitchView.isChecked = stopTime != null && stopTime > Instant.now()
            sleepTimerPickerView.isVisible = sleepTimerSwitchView.isChecked

            sleepTimerSwitchView.setOnCheckedChangeListener { _, isChecked ->
                WhiteNoiseService.stop(context)
                sleepTimerPickerView.isVisible = isChecked
            }

            sleepTimerPickerView.setOnDurationChangeListener {
                WhiteNoiseService.stop(context)
            }
        }

        useEffect(context, whiteNoiseButtonView, sleepTimerSwitchView, sleepTimerPickerView) {
            whiteNoiseButtonView.setOnClickListener {
                if (WhiteNoiseService.isRunning) {
                    WhiteNoiseService.stop(requireContext())
                } else {
                    val duration = sleepTimerPickerView.duration
                    WhiteNoiseService.play(
                        context,
                        SleepSound.PinkNoise,
                        if (sleepTimerSwitchView.isChecked) duration else null
                    )
                }
            }
        }

        useEffect(sleepTimerPickerView, runEveryCycle) {
            val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
            if (stopTime != null && stopTime > Instant.now()) {
                sleepTimerPickerView.updateDuration(Duration.between(Instant.now(), stopTime))
            }
        }
    }

}