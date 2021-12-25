package com.kylecorry.trail_sense.tools.flashlight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolFlashlightBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.StrobeService

class FragmentToolFlashlight : BoundFragment<FragmentToolFlashlightBinding>() {

    private var flashlightState = FlashlightState.Off
    private val flashlight by lazy { FlashlightHandler.getInstance(requireContext()) }
    private val intervalometer = Timer {
        update()
    }

    private val switchStateTimer = Timer {
        flashlight.set(selectedState)
    }

    private var selectedState = FlashlightState.On

    private val cache by lazy { Preferences(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hasFlashlight = Torch.isAvailable(requireContext())
        binding.flashlightOnBtn.isVisible = hasFlashlight
        binding.flashlightDial.isVisible = hasFlashlight
        binding.flashlightOnBtn.setOnClickListener {
            switchStateTimer.stop()
            toggle()
        }

        binding.screenFlashlightBtn.setOnClickListener {
            flashlight.set(FlashlightState.Off)
            findNavController().navigate(R.id.action_flashlight_to_screen_flashlight)
        }

        binding.flashlightDial.options = listOf(
            0.toString(),
            1.toString(),
            2.toString(),
            3.toString(),
            4.toString(),
            5.toString(),
            6.toString(),
            7.toString(),
            8.toString(),
            9.toString(),
            200.toString(),
            getString(R.string.sos)
        )
        binding.flashlightDial.range = 180f
        binding.flashlightDial.alignToTop = true
        binding.flashlightDial.background =
            Resources.androidBackgroundColorSecondary(requireContext())
        binding.flashlightDial.foreground = Resources.androidTextColorPrimary(requireContext())
        binding.flashlightDial.selectionChangeListener = {
            val isStrobe = it in 1..10

            if (isStrobe) {
                CustomUiUtils.disclaimer(
                    requireContext(),
                    getString(R.string.strobe_warning_title),
                    getString(R.string.strobe_warning_content),
                    getString(R.string.pref_fine_with_strobe),
                    considerShownIfCancelled = false,
                ) { cancelled ->
                    val frequency = if (it == 10) 200 else it
                    cache.putLong(StrobeService.STROBE_DURATION_KEY, 1000L / frequency)
                    selectedState = if (!cancelled) {
                        FlashlightState.Strobe
                    } else {
                        FlashlightState.On
                    }
                    changeMode()
                }
            } else {
                selectedState = when (it) {
                    11 -> FlashlightState.SOS
                    else -> FlashlightState.On
                }
                changeMode()
            }
        }
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
        switchStateTimer.stop()
    }

    private fun changeMode() {
        if (flashlight.getState() != FlashlightState.Off) {
            turnOff()
            switchStateTimer.once(400)
        }
    }

    private fun updateFlashlightUI() {
        binding.flashlightOnBtn.setState(flashlightState != FlashlightState.Off)
    }

    private fun toggle() {
        if (flashlight.getState() != FlashlightState.Off) {
            flashlight.set(FlashlightState.Off)
        } else {
            flashlight.set(selectedState)
        }
    }

    private fun turnOff() {
        flashlight.set(FlashlightState.Off)
    }


    private fun update() {
        flashlightState = flashlight.getState()
        updateFlashlightUI()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolFlashlightBinding {
        return FragmentToolFlashlightBinding.inflate(layoutInflater, container, false)
    }

}