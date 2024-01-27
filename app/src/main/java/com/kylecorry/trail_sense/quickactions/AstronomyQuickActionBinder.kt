package com.kylecorry.trail_sense.quickactions

import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.astronomy.ui.AstronomyFragment
import com.kylecorry.trail_sense.databinding.ActivityAstronomyBinding
import com.kylecorry.trail_sense.tools.ui.Tools

class AstronomyQuickActionBinder(
    private val fragment: AstronomyFragment,
    private val binding: ActivityAstronomyBinding,
    private val prefs: AstronomyPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(prefs.leftButton, binding.astronomyTitle.leftButton, fragment)
        val right = factory.create(prefs.rightButton, binding.astronomyTitle.rightButton, fragment)
        left.bind(fragment)
        right.bind(fragment)
    }

}