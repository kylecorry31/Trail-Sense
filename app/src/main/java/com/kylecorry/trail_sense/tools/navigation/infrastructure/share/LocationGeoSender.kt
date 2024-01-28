package com.kylecorry.trail_sense.tools.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.sol.science.geography.CoordinateFormat
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R

class LocationGeoSender(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate, format: CoordinateFormat?) {
        val intent = Intents.geo(location)
        Intents.openChooser(context, intent, context.getString(R.string.open_beacon_in_maps))
    }

}