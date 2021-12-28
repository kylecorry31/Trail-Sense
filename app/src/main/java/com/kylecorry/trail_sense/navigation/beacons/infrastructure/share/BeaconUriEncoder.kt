package com.kylecorry.trail_sense.navigation.beacons.infrastructure.share

import android.net.Uri
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.uri.GeoUri
import com.kylecorry.trail_sense.shared.uri.IUriEncoder

class BeaconUriEncoder : IUriEncoder<Beacon> {

    override fun encode(value: Beacon): Uri {
        val geo = GeoUri.from(value)
        return geo.uri
    }

    override fun decode(uri: Uri): Beacon? {
        val geo = GeoUri.from(uri) ?: return null
        return Beacon(
            0,
            geo.queryParameters.getOrDefault("label", ""),
            geo.coordinate,
            elevation = geo.altitude ?: geo.queryParameters.getOrDefault("ele", "").toFloatOrNull(),
            color = AppColor.Orange.color
        )
    }

}