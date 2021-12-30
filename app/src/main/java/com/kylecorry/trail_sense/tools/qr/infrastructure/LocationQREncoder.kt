package com.kylecorry.trail_sense.tools.qr.infrastructure

import android.net.Uri
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.uri.LocationUriEncoder

class LocationQREncoder : IQREncoder<Coordinate> {

    private val uriConverter = LocationUriEncoder()

    override fun encode(value: Coordinate): String {
        return uriConverter.encode(value).toString()
    }

    override fun decode(qr: String): Coordinate? {
        return uriConverter.decode(Uri.parse(qr))
    }
}