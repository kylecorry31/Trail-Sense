package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kylecorry.trail_sense.navigation.domain.Beacon

class BeaconGeoSender(private val context: Context): IBeaconSender {

    override fun send(beacon: Beacon) {
        val uri = Uri.parse("geo:${beacon.coordinate.latitude},${beacon.coordinate.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}