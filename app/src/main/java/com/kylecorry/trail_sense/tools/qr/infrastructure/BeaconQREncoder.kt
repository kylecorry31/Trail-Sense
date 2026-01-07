package com.kylecorry.trail_sense.tools.qr.infrastructure

import androidx.core.net.toUri
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconUriEncoder

class BeaconQREncoder : IQREncoder<Beacon> {

    private val uriConverter = BeaconUriEncoder()

    override fun encode(value: Beacon): String {
        return uriConverter.encode(value).toString()
    }

    override fun decode(qr: String): Beacon? {
        return uriConverter.decode(qr.toUri())
    }
}