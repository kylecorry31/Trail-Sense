package com.kylecorry.trail_sense.tools.beacons.infrastructure.share

import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

interface IBeaconSender {

    fun send(beacon: Beacon)

}