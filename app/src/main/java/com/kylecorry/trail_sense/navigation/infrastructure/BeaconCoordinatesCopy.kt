package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.navigation.domain.Beacon

class BeaconCoordinatesCopy(private val clipboard: Clipboard) : IBeaconSender {

    override fun send(beacon: Beacon) {
        val locString = beacon.coordinate.toString()
        clipboard.copy(locString, true)
    }

}