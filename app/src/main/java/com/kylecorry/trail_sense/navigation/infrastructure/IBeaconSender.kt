package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.navigation.domain.Beacon

interface IBeaconSender {

    fun send(beacon: Beacon)

}