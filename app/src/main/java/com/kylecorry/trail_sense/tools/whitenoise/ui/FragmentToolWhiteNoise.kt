package com.kylecorry.trail_sense.tools.whitenoise.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentToolWhiteNoiseBinding
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import java.time.Duration
import java.time.Instant

class FragmentToolWhiteNoise : BoundFragment<FragmentToolWhiteNoiseBinding>() {

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
        binding.sleepTimerSwitch.isChecked = stopTime != null && stopTime > Instant.now()
        binding.sleepTimerPicker.isVisible = binding.sleepTimerSwitch.isChecked

        if (stopTime != null && stopTime > Instant.now()){
            binding.sleepTimerPicker.updateDuration(Duration.between(Instant.now(), stopTime))
        }

        binding.sleepTimerSwitch.setOnCheckedChangeListener { _, isChecked ->
            WhiteNoiseService.stop(requireContext())
            binding.sleepTimerPicker.isVisible = isChecked
        }

        binding.sleepTimerPicker.setOnDurationChangeListener {
            WhiteNoiseService.stop(requireContext())
        }

        binding.whiteNoiseBtn.setOnClickListener {
            if (WhiteNoiseService.isRunning){
                WhiteNoiseService.stop(requireContext())
            } else {

                val duration = binding.sleepTimerPicker.duration

                if (binding.sleepTimerSwitch.isChecked && duration != null && !duration.isZero){
                    cache.putInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME, Instant.now().plus(duration))
                } else {
                    WhiteNoiseService.clearSleepTimer(requireContext())
                }

                WhiteNoiseService.start(requireContext())
            }
        }

        scheduleUpdates(INTERVAL_30_FPS)
    }


    override fun onUpdate() {
        super.onUpdate()
        binding.whiteNoiseBtn.setState(WhiteNoiseService.isRunning)
        val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
        if (stopTime != null && stopTime > Instant.now()){
            binding.sleepTimerPicker.updateDuration(Duration.between(Instant.now(), stopTime))
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolWhiteNoiseBinding {
        return FragmentToolWhiteNoiseBinding.inflate(layoutInflater, container, false)
    }

}