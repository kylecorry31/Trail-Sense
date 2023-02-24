package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences

class OverrideDeclinationStrategy(prefs: IDeclinationPreferences) : IDeclinationStrategy {
    private val declination = prefs.declinationOverride

    override fun getDeclination(): Float {
        return declination
    }
}