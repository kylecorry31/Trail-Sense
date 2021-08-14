package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.core.system.IntentUtils
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.R

class LocationGeoSender(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate) {
        val intent = IntentUtils.geo(location)
        IntentUtils.openChooser(context, intent, context.getString(R.string.open_beacon_in_maps))
    }

}