package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences

class OverrideDeclinationStrategy(private val prefs: IDeclinationPreferences) : IDeclinationStrategy {
    override fun getDeclination(): Float {
        return prefs.declinationOverride
    }
}