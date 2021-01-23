package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils

class BeaconGeoSender(private val context: Context): IBeaconSender {

    override fun send(beacon: Beacon) {
        val intent = IntentUtils.geo(beacon.coordinate)
        val chooser = Intent.createChooser(intent, context.getString(R.string.open_beacon_in_maps))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }
}