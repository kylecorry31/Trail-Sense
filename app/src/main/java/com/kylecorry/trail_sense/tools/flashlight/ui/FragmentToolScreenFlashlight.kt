package com.kylecorry.trail_sense.tools.flashlight.ui

import android.graphics.Color
import android.view.View
import android.widget.Button
import com.google.android.material.slider.BasicLabelFormatter
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.usePreference
import com.kylecorry.trail_sense.shared.extensions.useResumeEffect
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.Slider

class FragmentToolScreenFlashlight :
    TrailSenseReactiveFragment(R.layout.fragment_tool_screen_flashlight) {

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }

    // Held on the fragment so the volume buttons can adjust it from outside the render function
    private var brightness by state(DEFAULT_BRIGHTNESS)

    override fun update() {
        // Views
        val screenFlashlightView = useView<View>(R.id.screen_flashlight)
        val redWhiteSwitcherView = useView<View>(R.id.red_white_switcher)
        val brightnessSeekView = useView<Slider>(R.id.brightness_seek)
        val offButtonView = useView<Button>(R.id.off_btn)

        // Services
        val screenTorch = useMemo { ScreenTorch(requireActivity().window) }

        // State
        val (savedIsRed, setIsRed) = usePreference(
            "cache_red_light",
            IPreferences::getBoolean,
            IPreferences::putBoolean
        )
        val isRed = savedIsRed == true

        useResumeEffect(cache) {
            brightness = cache.getInt(getString(R.string.pref_screen_torch_brightness))
                ?: DEFAULT_BRIGHTNESS
        }

        // Light
        useEffect(screenTorch, brightness, resetOnResume) {
            screenTorch.on(map(brightness / 100f, 0f, 1f, 0.1f, 1f))
        }

        usePauseEffect(screenTorch) {
            screenTorch.off()
        }

        // View - Color
        useEffect(screenFlashlightView, redWhiteSwitcherView, isRed) {
            screenFlashlightView.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
            redWhiteSwitcherView.setBackgroundColor(if (isRed) Color.WHITE else Color.RED)
        }

        useEffect(redWhiteSwitcherView, isRed) {
            redWhiteSwitcherView.setOnClickListener {
                setIsRed(!isRed)
            }
        }

        // View - Brightness
        useEffect(brightnessSeekView) {
            brightnessSeekView.valueFrom = 0f
            brightnessSeekView.valueTo = 100f
            brightnessSeekView.setLabelFormatter(BasicLabelFormatter())
            brightnessSeekView.applyThinStyling()
            brightnessSeekView.addOnChangeListener { _, value, isFromUser ->
                if (isFromUser) {
                    saveBrightness(value.toInt())
                }
            }
        }

        useEffect(brightnessSeekView, brightness) {
            brightnessSeekView.value = brightness.toFloat()
        }

        // View - Off button
        useEffect(offButtonView, screenTorch) {
            offButtonView.setOnClickListener {
                screenTorch.off()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun saveBrightness(percent: Int) {
        cache.putInt(getString(R.string.pref_screen_torch_brightness), percent)
        brightness = percent
    }

    fun handleVolumeButtonPress(isVolumeUp: Boolean) {
        val change = if (isVolumeUp) 10 else -10
        saveBrightness((brightness + change).coerceIn(0, 100))
    }

    private companion object {
        const val DEFAULT_BRIGHTNESS = 100
    }
}
