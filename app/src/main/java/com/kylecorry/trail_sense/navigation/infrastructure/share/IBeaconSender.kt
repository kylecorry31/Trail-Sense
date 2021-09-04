package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.trail_sense.shared.beacons.Beacon

interface IBeaconSender {

    fun send(beacon: Beacon)

}