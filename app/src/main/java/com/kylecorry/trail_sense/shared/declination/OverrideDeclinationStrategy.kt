package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.trail_sense.shared.UserPreferences

class OverrideDeclinationStrategy(private val prefs: UserPreferences) : IDeclinationStrategy {
    override fun getDeclination(): Float {
        return prefs.declinationOverride
    }
}