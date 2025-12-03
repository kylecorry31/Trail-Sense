package com.kylecorry.trail_sense.tools.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat

class LocationGeoSender(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate, format: BuiltInCoordinateFormat?) {
        val intent = Intents.geo(location)
        Intents.openChooser(context, intent, context.getString(R.string.open_beacon_in_maps))
    }

}