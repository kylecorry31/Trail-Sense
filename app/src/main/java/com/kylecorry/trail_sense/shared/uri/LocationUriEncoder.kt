package com.kylecorry.trail_sense.shared.uri

import android.net.Uri
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.units.Coordinate

class LocationUriEncoder : IUriEncoder<Coordinate> {
    override fun encode(value: Coordinate): Uri {
        return GeoUri(value).uri
    }

    override fun decode(uri: Uri): Coordinate? {
        return GeoUri.from(uri)?.coordinate
    }
}