package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.astronomy.ui.AstronomyFragment
import com.kylecorry.trail_sense.databinding.ActivityAstronomyBinding
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.views.QuickActionNone

class AstronomyQuickActionBinder(
    private val fragment: AstronomyFragment,
    private val binding: ActivityAstronomyBinding,
    private val prefs: AstronomyPreferences
) : IQuickActionBinder {

    override fun bind() {
        getQuickActionButton(
            prefs.leftButton,
            binding.astronomyTitle.leftButton
        ).bind(fragment)

        getQuickActionButton(
            prefs.rightButton,
            binding.astronomyTitle.rightButton
        ).bind(fragment)
    }

    private fun getQuickActionButton(
        type: QuickActionType,
        button: ImageButton
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