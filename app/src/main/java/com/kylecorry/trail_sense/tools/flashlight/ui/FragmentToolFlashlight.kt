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
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolFlashlightBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.setOnProgressChangeListener
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class FragmentToolFlashlight : BoundFragment<FragmentToolFlashlightBinding>() {

    private var flashlightMode = FlashlightMode.Off
    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }
    private val flashlight by lazy { FlashlightSubsystem.getInstance(requireContext()) }
    private val intervalometer = Timer {
        update()
    }

    private var brightness = 1f

    private val switchStateTimer = Timer {
        turnOn()
    }

    private var selectedMode = FlashlightMode.Torch

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private var maxBrightness = 1
    private var hasBrightnessControl = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hasFlashlight = flashlight.isAvailable()
        binding.flashlightDialIndicator.isVisible = hasFlashlight
        binding.flashlightOnBtn.isVisible = hasFlashlight
        binding.flashlightDial.isVisible = hasFlashlight

        maxBrightness = flashlight.brightnessLevels
        hasBrightnessControl = maxBrightness > 0
        binding.brightnessSeek.max = maxBrightness
        updateBrightness()
        binding.brightnessSeek.isVisible = hasBrightnessControl
        binding.brightnessSeek.setOnProgressChangeListener { progress, fromUser ->
            if (fromUser) {
                flashlight.setBrightness(progress / maxBrightness.toFloat())
                turnOn()
            }
        }

        binding.flashlightOnBtn.setOnClickListener {
            switchStateTimer.stop()
            toggle()
        }

        binding.screenFlashlightBtn.setOnClickListener {
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
                ) { _, agreed ->
                    val frequency = if (it == 10) 200 else it
                    selectedMode = if (agreed) {
                        getStrobeMode(frequency)
                    } else {
                        FlashlightMode.Torch
                    }
                    turnOn()
                }
            } else {
                selectedMode = when (it) {
                    11 -> FlashlightMode.Sos
                    else -> FlashlightMode.Torch
                }
                turnOn()
            }
        }
    }

    private fun getStrobeMode(frequency: Int): FlashlightMode {
        return when (frequency) {
            1 -> FlashlightMode.Strobe1
            2 -> FlashlightMode.Strobe2
            3 -> FlashlightMode.Strobe3
            4 -> FlashlightMode.Strobe4
            5 -> FlashlightMode.Strobe5
            6 -> FlashlightMode.Strobe6
            7 -> FlashlightMode.Strobe7
            8 -> FlashlightMode.Strobe8
            9 -> FlashlightMode.Strobe9
            200 -> FlashlightMode.Strobe200
            else -> FlashlightMode.Torch
        }
    }

    override fun onResume() {
        super.onResume()
        flashlightMode = flashlight.getMode()
        updateFlashlightUI()
        intervalometer.interval(20)
        binding.flashlightDial.areHapticsEnabled = true
    }

    override fun onPause() {
        super.onPause()
        haptics.off()
        intervalometer.stop()
        switchStateTimer.stop()
        binding.flashlightDial.areHapticsEnabled = false
    }

    private fun updateFlashlightUI() {
        binding.flashlightOnBtn.setState(flashlightMode != FlashlightMode.Off)
        updateTimer()
    }

    private fun updateBrightness(value: Float? = null) {
        if (hasBrightnessControl) {
            brightness = value ?: prefs.flashlight.brightness
            binding.brightnessSeek.progress = (brightness * maxBrightness).roundToInt()
        } else {
            brightness = 1f
        }
        flashlight.setBrightness(brightness)
    }

    fun toggle() {
        haptics.click()
        if (flashlight.getMode() != FlashlightMode.Off) {
            turnOff()
        } else {
            turnOn()
        }
    }

    private fun turnOn() {
        flashlight.set(selectedMode)
    }

    private fun turnOff() {
        flashlight.set(FlashlightMode.Off)
    }

    private fun update() {
        flashlightMode = flashlight.getMode()
        updateFlashlightUI()
    }

    private fun updateTimer() {
        if (!prefs.flashlight.shouldTimeout) {
            binding.flashlightOnBtn.setText(null)
            return
        }

        val instant = cache.getInstant(getString(R.string.pref_flashlight_timeout_instant))
        val duration = if (instant != null && instant.isAfter(Instant.now())) {
            Duration.between(Instant.now(), instant)
        } else {
            prefs.flashlight.timeout
        }

        binding.flashlightOnBtn.setText(
            formatter.formatDuration(
                duration,
                short = false,
                includeSeconds = true
            )
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolFlashlightBinding {
        return FragmentToolFlashlightBinding.inflate(layoutInflater, container, false)
    }

}