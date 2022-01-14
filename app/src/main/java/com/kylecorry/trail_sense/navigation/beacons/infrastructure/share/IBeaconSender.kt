package com.kylecorry.trail_sense.navigation.beacons.infrastructure.share

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

interface IBeaconSender {

    fun send(beacon: Beacon)

}