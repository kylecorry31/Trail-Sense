package com.kylecorry.trail_sense.tools.flashlight.ui

import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.BasicLabelFormatter
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.useResumeEffect
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.DialSelectView
import com.kylecorry.trail_sense.shared.views.Slider
import com.kylecorry.trail_sense.shared.views.TileButton
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import java.time.Duration
import java.time.Instant

class FragmentToolFlashlight : TrailSenseReactiveFragment(R.layout.fragment_tool_flashlight) {

    private var selectedMode = FlashlightMode.Torch

    override fun update() {
        val onButton = useView<TileButton>(R.id.flashlight_on_btn)
        val screenButton = useView<TileButton>(R.id.screen_flashlight_btn)
        val dial = useView<DialSelectView>(R.id.flashlight_dial)
        val indicator = useView<View>(R.id.flashlight_dial_indicator)
        val brightnessSlider = useView<Slider>(R.id.brightness_seek)

        val context = useAndroidContext()
        val flashlight = useService<FlashlightSubsystem>()
        val haptics = useService<HapticSubsystem>()
        val prefs = useService<UserPreferences>()
        val cache = useService<PreferencesSubsystem>().preferences
        val formatter = useService<FormatService>()
        val modeFlow = useMemo(flashlight) { flashlight.mode.replay().flow }
        val mode = useFlow(modeFlow, state = BackgroundMinimumState.Resumed) ?: flashlight.getMode()
        val hasFlashlight = flashlight.isAvailable()
        val maxBrightness = flashlight.brightnessLevels
        val options = useMemo {
            listOf(
                FlashlightOption(FlashlightMode.Torch, "0"),
                FlashlightOption(FlashlightMode.Strobe1, "1", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe2, "2", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe3, "3", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe4, "4", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe5, "5", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe6, "6", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe7, "7", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe8, "8", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe9, "9", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Strobe200, "200", requiresDisclaimer = true),
                FlashlightOption(FlashlightMode.Sos, getString(R.string.sos))
            )
        }

        useEffect(onButton, dial, indicator, hasFlashlight) {
            onButton.isVisible = hasFlashlight
            dial.isVisible = hasFlashlight
            indicator.isVisible = hasFlashlight
        }

        useEffect(brightnessSlider, maxBrightness) {
            brightnessSlider.valueFrom = 0f
            brightnessSlider.valueTo = maxBrightness.coerceAtLeast(1).toFloat()
            brightnessSlider.stepSize = 1f
            val basicFormatter = BasicLabelFormatter()
            brightnessSlider.setLabelFormatter {
                basicFormatter.getFormattedValue(100 * it / maxBrightness.coerceAtLeast(1))
            }
            brightnessSlider.isVisible = maxBrightness > 0
            val brightness = if (maxBrightness > 0) prefs.flashlight.brightness else 1f
            brightnessSlider.value = (brightness * maxBrightness).safeRoundToInt().toFloat()
            flashlight.setBrightness(brightness)
        }

        useEffect(brightnessSlider) {
            brightnessSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser && flashlight.brightnessLevels > 0) {
                    flashlight.setBrightness(value / flashlight.brightnessLevels.toFloat())
                    flashlight.set(selectedMode)
                }
            }
        }

        useEffect(onButton, screenButton) {
            onButton.setOnClickListener {
                toggle()
            }
            screenButton.setOnClickListener {
                findNavController().navigate(R.id.action_flashlight_to_screen_flashlight)
            }
        }

        useEffect(dial, options) {
            dial.selectedColor = Resources.getPrimaryColor(context)
            dial.options = options.map { it.label }
            dial.range = 180f
            dial.alignToTop = true
            dial.background = Resources.getAndroidColorAttr(
                context, com.google.android.material.R.attr.colorSurfaceContainer
            )
            dial.foreground = Resources.getAndroidColorAttr(
                context, com.google.android.material.R.attr.colorOnSurface
            )
            dial.selectionChangeListener = { index ->
                val option = options.getOrNull(index) ?: options.first()
                if (option.requiresDisclaimer) {
                    CustomUiUtils.disclaimer(
                        context,
                        getString(R.string.strobe_warning_title),
                        getString(R.string.strobe_warning_content),
                        getString(R.string.pref_fine_with_strobe),
                        considerShownIfCancelled = false,
                    ) { _, agreed ->
                        selectedMode = if (agreed) {
                            option.mode
                        } else {
                            FlashlightMode.Torch
                        }
                        flashlight.set(selectedMode)
                    }
                } else {
                    selectedMode = option.mode
                    flashlight.set(selectedMode)
                }
            }
        }

        useResumeEffect(dial, options) {
            selectedMode = if (flashlight.getMode() != FlashlightMode.Off) {
                flashlight.selectedMode
            } else {
                FlashlightMode.Torch
            }
            val index = options.indexOfFirst { it.mode == selectedMode }
            dial.selected = index
            dial.scrollToOption(index)
            dial.areHapticsEnabled = true
        }

        usePauseEffect(dial, haptics) {
            haptics.off()
            dial.areHapticsEnabled = false
        }

        useEffect(onButton, dial, mode) {
            onButton.setState(mode != FlashlightMode.Off)
            if (mode != FlashlightMode.Off) {
                selectedMode = mode
                val index = options.indexOfFirst { it.mode == selectedMode }
                dial.selected = index
                dial.scrollToOption(index)
            }
        }

        useEffectWithCleanup(onButton, mode, resetOnResume) {
            val updateCountdown = {
                if (prefs.flashlight.shouldTimeout) {
                    onButton.setText(
                        formatter.formatDuration(
                            getRemainingTimeout(cache, prefs),
                            short = false,
                            includeSeconds = true
                        )
                    )
                } else {
                    onButton.setText(null)
                }
            }
            updateCountdown()
            val timer = CoroutineTimer { updateCountdown() }
            if (mode != FlashlightMode.Off && prefs.flashlight.shouldTimeout) {
                // Line the timer up to the second so it better reflects the countdown
                val initialDelayMillis = getRemainingTimeout(cache, prefs).toMillis() % 1000
                timer.interval(1000, initialDelayMillis = initialDelayMillis)
            }
            return@useEffectWithCleanup { 
                updateCountdown()
                timer.stop()
            }
        }
    }

    private fun getRemainingTimeout(cache: IPreferences, prefs: UserPreferences): Duration {
        val now = Instant.now()
        val instant = cache.getInstant(getString(R.string.pref_flashlight_timeout_instant))
        return if (instant != null && instant.isAfter(now)) {
            Duration.between(now, instant)
        } else {
            prefs.flashlight.timeout
        }
    }

    fun toggle() {
        val flashlight = FlashlightSubsystem.getInstance(requireContext())
        HapticSubsystem.getInstance(requireContext()).click()
        flashlight.set(
            if (flashlight.getMode() != FlashlightMode.Off) FlashlightMode.Off else selectedMode
        )
    }

    private data class FlashlightOption(
        val mode: FlashlightMode,
        val label: String,
        val requiresDisclaimer: Boolean = false
    )
}
