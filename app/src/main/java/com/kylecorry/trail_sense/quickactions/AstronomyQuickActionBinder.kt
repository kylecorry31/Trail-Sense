package com.kylecorry.trail_sense.quickactions

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.astronomy.ui.AstronomyFragment
import com.kylecorry.trail_sense.databinding.ActivityAstronomyBinding
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.tools.flashlight.ui.QuickActionFlashlight
import com.kylecorry.trail_sense.tools.whistle.ui.QuickActionWhistle
import com.kylecorry.trail_sense.tools.whitenoise.ui.QuickActionWhiteNoise

class AstronomyQuickActionBinder(
    private val fragment: AstronomyFragment,
    private val binding: ActivityAstronomyBinding,
    private val prefs: AstronomyPreferences
) : IQuickActionBinder {

    override fun bind() {
        getQuickActionButton(
            prefs.leftQuickAction,
            binding.astronomyLeftQuickAction
        ).bind(fragment)

        getQuickActionButton(
            prefs.rightQuickAction,
            binding.astronomyRightQuickAction
        ).bind(fragment)
    }

    private fun getQuickActionButton(
        type: QuickActionType,
        button: FloatingActionButton
    ): QuickActionButton {
        return when (type) {
            QuickActionType.Whistle -> QuickActionWhistle(button, fragment)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, fragment)
            QuickActionType.WhiteNoise -> QuickActionWhiteNoise(button, fragment)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, fragment)
            else -> QuickActionNone(button, fragment)
        }
    }
}