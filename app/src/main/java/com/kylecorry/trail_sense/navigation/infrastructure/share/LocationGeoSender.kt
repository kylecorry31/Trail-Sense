package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils

class LocationGeoSender(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate) {
        val intent = IntentUtils.geo(location)
        IntentUtils.openChooser(context, intent, context.getString(R.string.open_beacon_in_maps))
    }

}