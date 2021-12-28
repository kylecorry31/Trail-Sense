package com.kylecorry.trail_sense.tools.qr.infrastructure

import android.net.Uri
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.share.BeaconGeoUriConverter

class BeaconQREncoder : IQREncoder<Beacon> {

    private val uriConverter = BeaconGeoUriConverter()

    override fun encode(value: Beacon): String {
        return uriConverter.encode(value).toString()
    }

    override fun decode(qr: String): Beacon? {
        return uriConverter.decode(Uri.parse(qr))
    }
}