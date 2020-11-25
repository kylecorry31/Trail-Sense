package com.kylecorry.trail_sense.tools.flashlight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolFlashlightBinding
import com.kylecorry.trail_sense.databinding.FragmentToolWhistleBinding
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class FragmentToolFlashlight: Fragment() {

    private var flashlightState = FlashlightState.Off
    private val flashlight by lazy { FlashlightHandler(requireContext()) }
    private val intervalometer = Intervalometer {
        update()
    }

    private var _binding: FragmentToolFlashlightBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolFlashlightBinding.inflate(inflater, container, false)
        binding.flashlightBtn.setOnClickListener {
            flashlightState = getNextFlashlightState(flashlightState)
            flashlight.set(flashlightState)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        flashlightState = flashlight.getState()
        updateFlashlightUI()
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }


    private fun getNextFlashlightState(currentState: FlashlightState): FlashlightState {
        return flashlight.getNextState(currentState)
    }

    private fun updateFlashlightUI() {
        when (flashlightState) {
            FlashlightState.On -> {
                binding.flashlightBtn.setImageResource(R.drawable.flashlight)
                UiUtils.setButtonState(
                    binding.flashlightBtn,
                    true,
                    UiUtils.color(requireContext(), R.color.colorPrimary),
                    UiUtils.color(requireContext(), R.color.colorSecondary)
                )
            }
            FlashlightState.SOS -> {
                binding.flashlightBtn.setImageResource(R.drawable.flashlight_sos)
                UiUtils.setButtonState(
                    binding.flashlightBtn,
                    true,
                    UiUtils.color(requireContext(), R.color.colorPrimary),
                    UiUtils.color(requireContext(), R.color.colorSecondary)
                )
            }
            else -> {
                binding.flashlightBtn.setImageResource(R.drawable.flashlight)
                UiUtils.setButtonState(
                    binding.flashlightBtn,
                    false,
                    UiUtils.color(requireContext(), R.color.colorPrimary),
                    UiUtils.color(requireContext(), R.color.colorSecondary)
                )
            }
        }
    }


    private fun update(){
        flashlightState = flashlight.getState()
        updateFlashlightUI()
    }

}