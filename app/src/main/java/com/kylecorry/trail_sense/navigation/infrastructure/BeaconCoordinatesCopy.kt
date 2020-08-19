package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.UserPreferences

class BeaconCoordinatesCopy(private val clipboard: Clipboard, private val prefs: UserPreferences) : IBeaconSender {

    override fun send(beacon: Beacon) {
        val locString = prefs.navigation.formatLocation(beacon.coordinate)
        clipboard.copy(locString, true)
    }

}