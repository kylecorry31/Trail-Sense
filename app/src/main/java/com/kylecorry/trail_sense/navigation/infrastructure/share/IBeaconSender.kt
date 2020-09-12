package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.trailsensecore.domain.navigation.Beacon

interface IBeaconSender {

    fun send(beacon: Beacon)

}