package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences

class DeclinationProvider {

    fun getDeclinationStrategy(prefs: IDeclinationPreferences, gps: IGPS? = null): IDeclinationStrategy {
        return if (gps == null || !prefs.useAutoDeclination) {
            OverrideDeclinationStrategy(prefs)
        } else {
            GPSDeclinationStrategy(gps)
        }
    }

}