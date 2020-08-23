package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.UserPreferences

class BeaconCopy(private val clipboard: Clipboard, private val prefs: UserPreferences) :
    IBeaconSender {

    override fun send(beacon: Beacon) {
        val text = "${beacon.name}: ${prefs.navigation.formatLocation(beacon.coordinate)}"
        clipboard.copy(text, true)
    }

}