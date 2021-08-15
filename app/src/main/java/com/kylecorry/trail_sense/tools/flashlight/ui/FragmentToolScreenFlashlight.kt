package com.kylecorry.trail_sense.tools.flashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.trail_sense.databinding.FragmentToolScreenFlashlightBinding

class FragmentToolScreenFlashlight : BoundFragment<FragmentToolScreenFlashlightBinding>() {

    private val flashlight by lazy { ScreenTorch(requireActivity().window) }
    private val cache by lazy { Preferences(requireContext()) }

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

        if (cache.getBoolean("cache_red_light") == null) {
            cache.putBoolean("cache_red_light", false)
        }

        if (cache.getBoolean("cache_red_light") == true) {
            binding.screenFlashlight.setBackgroundColor(Color.RED)
            binding.redWhiteSwitcher.setBackgroundColor(Color.WHITE)
        } else {
            binding.screenFlashlight.setBackgroundColor(Color.WHITE)
            binding.redWhiteSwitcher.setBackgroundColor(Color.RED)
        }

        binding.redWhiteSwitcher.setOnClickListener {
            if (cache.getBoolean("cache_red_light") == true) {
                binding.screenFlashlight.setBackgroundColor(Color.WHITE)
                binding.redWhiteSwitcher.setBackgroundColor(Color.RED)
                cache.putBoolean("cache_red_light", false)
            } else {
                binding.screenFlashlight.setBackgroundColor(Color.RED)
                binding.redWhiteSwitcher.setBackgroundColor(Color.WHITE)
                cache.putBoolean("cache_red_light", true)
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