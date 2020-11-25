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
            if (flashlight.getState() == FlashlightState.On){
                flashlight.set(FlashlightState.Off)
            } else {
                flashlight.set(FlashlightState.On)
            }
        }
        binding.sosBtn.setOnClickListener {
            if (flashlight.getState() == FlashlightState.SOS){
                flashlight.set(FlashlightState.Off)
            } else {
                flashlight.set(FlashlightState.SOS)
            }
        }

        binding.strobeBtn.setOnClickListener {
            if (flashlight.getState() == FlashlightState.Strobe){
                flashlight.set(FlashlightState.Off)
            } else {
                flashlight.set(FlashlightState.Strobe)
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
        flashlightState = flashlight.getState()
        updateFlashlightUI()
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun updateFlashlightUI() {
        UiUtils.setButtonState(
            binding.flashlightBtn,
            flashlightState == FlashlightState.On,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )

        UiUtils.setButtonState(
            binding.sosBtn,
            flashlightState == FlashlightState.SOS,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )

        UiUtils.setButtonState(
            binding.strobeBtn,
            flashlightState == FlashlightState.Strobe,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
    }


    private fun update(){
        flashlightState = flashlight.getState()
        updateFlashlightUI()
    }

}