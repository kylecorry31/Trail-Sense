package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.share.BeaconGeoUriConverter
import com.kylecorry.trail_sense.shared.uri.GeoUri

object AppUtils {

    fun placeBeacon(context: Context, geo: GeoUri) {
        val intent =
            Intents.localIntent(context, "com.kylecorry.trail_sense.PLACE_BEACON").apply {
                data = geo.uri
            }
        ContextCompat.startActivity(context, intent, null)
    }

    fun placeBeacon(context: Context, beacon: Beacon) {
        // TODO: Just place the beacon and return an ID or show a snackbar
        val encoder = BeaconGeoUriConverter()
        val intent =
            Intents.localIntent(context, "com.kylecorry.trail_sense.PLACE_BEACON").apply {
                data = encoder.encode(beacon)
            }
        ContextCompat.startActivity(context, intent, null)
    }

}