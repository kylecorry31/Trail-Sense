package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.core.view.isVisible
import com.google.android.material.materialswitch.MaterialSwitch
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.DurationInputView
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import com.kylecorry.trail_sense.shared.views.TileButton
import com.kylecorry.trail_sense.shared.withId
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
        val soundSelectorView = useView<MaterialSpinnerView>(R.id.sound_spinner)

        // Services
        val cache = useService<PreferencesSubsystem>().preferences
        val context = useAndroidContext()

        // State
        val soundTypes = useMemo {
            listOf(
                SleepSound.WhiteNoise to getString(R.string.sleep_sound_white_noise),
                SleepSound.PinkNoise to getString(R.string.sleep_sound_pink_noise),
                SleepSound.BrownNoise to getString(R.string.sleep_sound_brown_noise),
                SleepSound.Crickets to getString(R.string.sleep_sound_crickets),
                SleepSound.CricketsNoChirp to getString(R.string.sleep_sound_crickets_no_chirping),
                SleepSound.OceanWaves to getString(R.string.sleep_sound_ocean_waves),
                SleepSound.Fan to getString(R.string.sleep_sound_fan)
            )
        }

        // Effects
        useEffect(whiteNoiseButtonView, WhiteNoiseService.isRunning) {
            whiteNoiseButtonView.setState(WhiteNoiseService.isRunning)
        }

        useEffect(soundSelectorView, soundTypes, cache) {
            soundSelectorView.setItems(soundTypes.map { it.second })
            val lastSleepSoundId =
                cache.getLong(WhiteNoiseService.CACHE_KEY_SLEEP_SOUND_ID) ?: SleepSound.PinkNoise.id
            val lastSleepSound = SleepSound.entries.withId(lastSleepSoundId) ?: SleepSound.PinkNoise
            val index = soundTypes.indexOfFirst { it.first == lastSleepSound }
            if (index >= 0) {
                soundSelectorView.setSelection(index)
            } else {
                soundSelectorView.setSelection(0)
            }

            soundSelectorView.setOnItemSelectedListener { position ->
                val selectedSound = soundTypes[position ?: 0].first
                cache.putLong(WhiteNoiseService.CACHE_KEY_SLEEP_SOUND_ID, selectedSound.id)
                WhiteNoiseService.stop(context)
            }
        }

        useEffect(context, cache, sleepTimerSwitchView, sleepTimerPickerView) {
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
                        null,
                        if (sleepTimerSwitchView.isChecked) duration else null
                    )
                }
            }
        }

        useEffect(sleepTimerPickerView, cache, runEveryCycle) {
            val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
            if (stopTime != null && stopTime > Instant.now()) {
                sleepTimerPickerView.updateDuration(Duration.between(Instant.now(), stopTime))
            }
        }
    }

}