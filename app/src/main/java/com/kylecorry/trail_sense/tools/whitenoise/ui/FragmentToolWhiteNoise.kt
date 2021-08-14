package com.kylecorry.trail_sense.tools.whitenoise.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.databinding.FragmentToolWhiteNoiseBinding
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import java.time.Duration
import java.time.Instant

class FragmentToolWhiteNoise : BoundFragment<FragmentToolWhiteNoiseBinding>() {

    private val intervalometer = Timer {
        update()
    }

    private val cache by lazy { Cache(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
        binding.sleepTimerSwitch.isChecked = stopTime != null && stopTime > Instant.now()
        binding.sleepTimerPicker.isVisible = binding.sleepTimerSwitch.isChecked

        if (stopTime != null && stopTime > Instant.now()){
            binding.sleepTimerPicker.updateDuration(Duration.between(Instant.now(), stopTime))
        }

        binding.sleepTimerSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
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
                    cache.remove(WhiteNoiseService.CACHE_KEY_OFF_TIME)
                }

                WhiteNoiseService.start(requireContext())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun update() {
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