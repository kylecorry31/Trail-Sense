package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sharing.MapSiteService

class LocationSharesheet(private val context: Context) : ILocationSender {

    private val mapService = MapSiteService()
    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatService.getInstance(context) }

    override fun send(location: Coordinate, format: CoordinateFormat?) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getShareString(location, format ?: prefs.navigation.coordinateFormat))
            type = "text/plain"
        }
        Intents.openChooser(context, intent, context.getString(R.string.share_action_send))
    }

    private fun getShareString(coordinate: Coordinate, format: CoordinateFormat): String {
        val location = formatService.formatLocation(coordinate, CoordinateFormat.DecimalDegrees)
        val mapUrl = mapService.getUrl(coordinate, prefs.mapSite)

        if (format == CoordinateFormat.DecimalDegrees){
            return "${location}\n\n${
                context.getString(
                    R.string.maps
                )
            }: $mapUrl"
        }

        val coordinateUser = formatService.formatLocation(coordinate, format)
        return "${location}\n\n${formatService.formatCoordinateType(format)}: ${coordinateUser}\n\n${
            context.getString(
                R.string.maps
            )
        }: $mapUrl"
    }

}