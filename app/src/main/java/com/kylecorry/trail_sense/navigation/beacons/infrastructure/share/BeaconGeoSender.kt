package com.kylecorry.trail_sense.navigation.beacons.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

class BeaconGeoSender(private val context: Context): IBeaconSender {

    override fun send(beacon: Beacon) {
        val intent = Intents.geo(beacon.coordinate)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooser = Intent.createChooser(intent, context.getString(R.string.open_beacon_in_maps))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }
}