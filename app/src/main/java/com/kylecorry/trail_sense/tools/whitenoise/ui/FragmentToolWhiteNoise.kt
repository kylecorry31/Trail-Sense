package com.kylecorry.trail_sense.tools.whitenoise.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWhiteNoiseBinding
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class FragmentToolWhiteNoise : Fragment() {

    private val intervalometer = Intervalometer {
        update()
    }

    private var _binding: FragmentToolWhiteNoiseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolWhiteNoiseBinding.inflate(inflater, container, false)
        binding.whiteNoiseBtn.setOnClickListener {
            if (WhiteNoiseService.isOn(requireContext())){
                WhiteNoiseService.stop(requireContext())
            } else {
                WhiteNoiseService.start(requireContext())
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        UiUtils.setButtonState(
            binding.whiteNoiseBtn,
            WhiteNoiseService.isOn(requireContext()),
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
    }

}