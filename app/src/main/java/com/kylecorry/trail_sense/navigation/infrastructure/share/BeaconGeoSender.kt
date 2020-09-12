package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils

class BeaconGeoSender(private val context: Context): IBeaconSender {

    override fun send(beacon: Beacon) {
        context.startActivity(IntentUtils.geo(beacon.coordinate))
    }
}