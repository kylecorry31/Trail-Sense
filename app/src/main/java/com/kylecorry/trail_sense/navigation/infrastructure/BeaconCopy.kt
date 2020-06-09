package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.navigation.domain.Beacon

class BeaconCopy(private val clipboard: Clipboard) : IBeaconSender {

    override fun send(beacon: Beacon) {
        val text = "${beacon.name}: ${beacon.coordinate}"
        clipboard.copy(text, true)
    }

}