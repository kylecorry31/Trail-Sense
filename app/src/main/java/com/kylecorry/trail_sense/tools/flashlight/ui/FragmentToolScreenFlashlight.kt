package com.kylecorry.trail_sense.tools.flashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.databinding.FragmentToolScreenFlashlightBinding
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.ScreenFlashlight
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class FragmentToolScreenFlashlight: BoundFragment<FragmentToolScreenFlashlightBinding>() {

    private val flashlight by lazy { ScreenFlashlight(requireActivity().window) }
    private var redLight: Boolean = false

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolScreenFlashlightBinding {
        return FragmentToolScreenFlashlightBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.offBtn.setOnClickListener {
            flashlight.off()
            requireActivity().onBackPressed()
        }

        binding.redWhiteSwitcher.setOnClickListener {
            if (redLight) {
                binding.screenFlashlight.setBackgroundColor(Color.WHITE)
                binding.redWhiteSwitcher.setBackgroundColor(Color.RED)
                redLight = false
            }
            else {
                binding.screenFlashlight.setBackgroundColor(Color.RED)
                binding.redWhiteSwitcher.setBackgroundColor(Color.WHITE)
                redLight = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        flashlight.on()
    }

    override fun onPause() {
        super.onPause()
        flashlight.off()
    }

}